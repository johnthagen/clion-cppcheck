package com.github.johnthagen.cppcheck;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CppCheckInspectionImpl {
    private static ProblemHighlightType severityToHighlightType(@NotNull final String severity) {
        switch (severity) {
            case "error":
                return ProblemHighlightType.GENERIC_ERROR;
            case "warning":
                return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
            case "style":
            case "performance":
            case "portability":
                return ProblemHighlightType.WEAK_WARNING;
            case "information":
                return ProblemHighlightType.INFORMATION;

            // If the severity is not understood (changes in Cppcheck), return ERROR.
            default:
                return ProblemHighlightType.ERROR;
        }
    }

    private static final String INCONCLUSIVE_TEXT = ":inconclusive";

    @NotNull
    public static List<ProblemDescriptor> parseOutput(@NotNull PsiFile psiFile,
                                                @NotNull InspectionManager manager,
                                                @NotNull Document document,
                                                @NotNull String cppcheckOutput,
                                                @NotNull String sourceFileName) {
        List<ProblemDescriptor> descriptors = new ArrayList<>();
        Scanner scanner = new Scanner(cppcheckOutput);

        //Notifications.Bus.notify(new Notification("cppcheck",
        //                                          "Info",
        //                                          psiFile.getVirtualFile().getCanonicalPath() + "\n" +
        //                                          cppcheckOutput,
        //                                          NotificationType.INFORMATION));

        // Example output line:
        // [C:\Users\John Hagen\ClionProjects\test\main.cpp:12]: (style) Variable 'a' is not assigned a value.
        // [main.cpp:12] -> [main.cpp:14]: (performance) Variable 'a' is reassigned a value before the old one has been used.
        // One line:
        //  [C:\Users\User\AppData\Local\Temp\___1main.cpp:14]: (warning:inconclusive) accessMoved: Access of moved variable 'a'.
        //  [C:\Users\User\AppData\Local\Temp\___1main.cpp:14]: (style) unreadVariable: Variable 'name' is assigned a value that is never used.
        Pattern pattern = Pattern.compile("^\\[(.+?):(\\d+)](?:\\s+->\\s+\\[.+])?:\\s+\\((\\w+:?\\w+)\\)\\s+(.+)");

        String line;
        while (scanner.hasNext()) {
            line = scanner.nextLine();
            Matcher matcher = pattern.matcher(line);

            if (!matcher.matches()) {
                continue;
            }

            final String fileName = Paths.get(matcher.group(1)).getFileName().toString();
            int lineNumber = Integer.parseInt(matcher.group(2), 10);
            final String severity = matcher.group(3);
            final String errorMessage = matcher.group(4);

            // If a .c or .cpp file #include's header files, Cppcheck will also run on the header files and print
            // any errors. These errors don't apply to the current .cpp field and should not be drawn. They can
            // be distinguished by checking the file name.
            // Example:
            //   Checking Test.cpp ...
            //   [Test.h:2]: (style) Unused variable: x
            //   [Test.cpp:3]: (style) Unused variable: y
            if (!fileName.equals(sourceFileName)) {
                continue;
            }

            // Cppcheck error or parsing error.
            if (lineNumber <= 0 || lineNumber > document.getLineCount()) {
                continue;
            }

            // Document counts lines starting at 0, rather than 1 like in cppcheck.
            lineNumber -= 1;

            final int lineStartOffset = DocumentUtil.getFirstNonSpaceCharOffset(document, lineNumber);
            final int lintEndOffset = document.getLineEndOffset(lineNumber);

            final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    psiFile,
                    TextRange.create(lineStartOffset, lintEndOffset),
                    "Cppcheck: (" + severity + ") " + errorMessage,
                    severityToHighlightType(severity.replace(INCONCLUSIVE_TEXT, "")),
                    true);
            descriptors.add(problemDescriptor);
        }
        return descriptors;
    }

    private static final int TIMEOUT_MS = 60 * 1000;

    public static String executeCommandOnFile(@NotNull final String command,
                                               @NotNull final String options,
                                               @NotNull final String filePath,
                                               final String cppcheckMisraPath) throws CppcheckError, ExecutionException {

        if (options.contains("--template")) {
            throw new CppcheckError("Cppcheck options contain --template field. " +
                    "Please remove this, the plugin defines its own.");
        }

        final GeneralCommandLine cmd = new GeneralCommandLine()
                .withExePath(command)
                .withParameters(ParametersListUtil.parse(
                        "--template=\"[{file}:{line}]: ({severity}{inconclusive:" + INCONCLUSIVE_TEXT +
                                "}) {id}: {message}\""))
                .withParameters(ParametersListUtil.parse(options))
                .withParameters(ParametersListUtil.parse(filePath));

        // Need to be able to get python from the system env
        if (cppcheckMisraPath != null && !cppcheckMisraPath.isEmpty()) {
            cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM);
        }

        final CapturingProcessHandler processHandler = new CapturingProcessHandler(cmd);
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        final ProcessOutput output = processHandler.runProcessWithProgressIndicator(
                indicator != null ? indicator : new EmptyProgressIndicator(),
                TIMEOUT_MS);

        if (output.isCancelled()) {
            throw new ProcessCanceledException();
        }

        if (output.isTimeout()) {
            throw new CppcheckError("Timeout\n"
                    + cmd.getCommandLineString());
        }

        if (output.getExitCode() != 0) {
            throw new CppcheckError("Exit Code " + output.getExitCode() + "\n" +
                    "stdout: " + output.getStdout() + "\n" +
                    cmd.getCommandLineString());
        }

        if (cppcheckMisraPath != null && !cppcheckMisraPath.isEmpty()) {
            if (output.getStdout().contains("Bailing out from checking")) {
                // MISRA Mode and something went wrong with the misra addon
                throw new CppcheckError("MISRA Bail\n" +
                        cmd.getCommandLineString() + "\n" +
                        "stdout: " + output.getStdout() + "\n"+
                        "stderr: " + output.getStderr());
            }
        }

        return output.getStderr();
    }
}
