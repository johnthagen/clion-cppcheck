package com.github.johnthagen.cppcheck;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CppcheckInspection extends LocalInspectionTool {
    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file,
                                         @NotNull InspectionManager manager,
                                         boolean isOnTheFly) {
        final String cppcheckPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_PATH);
        String cppcheckOptions = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_OPTIONS);

        final String cppcheckMisraPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_MISRA_PATH);
        if (cppcheckMisraPath != null && !cppcheckMisraPath.isEmpty()) {
            cppcheckOptions = String.format("%s --addon=%s", cppcheckOptions, cppcheckMisraPath);
        }

        final VirtualFile vFile = file.getVirtualFile();
        if (vFile == null || !isCFamilyFile(vFile)) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        if (cppcheckPath == null || cppcheckPath.isEmpty()) {
            StatusBar.Info.set("[!] Error: Please set path of cppcheck in File->Settings->Cppcheck Configuration",
                    file.getProject());
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        final Document document = FileDocumentManager.getInstance().getDocument(vFile);
        if (document == null || document.getLineCount() == 0) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        File tempFile = null;
        try {
            tempFile = FileUtil.createTempFile("", vFile.getName(), true);
            FileUtil.writeToFile(tempFile, document.getText());
            String cppcheckOutput =
                    executeCommandOnFile(cppcheckPath, prependIncludeDir(cppcheckOptions, vFile),
                            tempFile.getAbsolutePath(), cppcheckMisraPath);

            if (!cppcheckOutput.isEmpty()) {
                List<ProblemDescriptor> descriptors = parseOutput(file, manager, document, cppcheckOutput,
                        tempFile.getName());
                return descriptors.toArray(new ProblemDescriptor[0]);
            }
        } catch (ExecutionException | CppcheckError | IOException ex) {
            Notifications.Bus.notify(new Notification("Cppcheck",
                    "Cppcheck execution failed.",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    NotificationType.ERROR));
            ex.printStackTrace();
        } finally {
            if (tempFile != null) {
                FileUtil.delete(tempFile);
            }
        }

        return ProblemDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    private static String prependIncludeDir(@NotNull String cppcheckOptions, @NotNull VirtualFile vFile) {
        final VirtualFile dir = vFile.getParent();
        if (dir == null) {
            return cppcheckOptions;
        }
        final String path = dir.getCanonicalPath();
        if (path == null) {
            return cppcheckOptions;
        }
        return String.format("-I\"%s\" %s", path, cppcheckOptions);
    }

    @NotNull
    private List<ProblemDescriptor> parseOutput(@NotNull PsiFile psiFile,
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

            ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
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
    private static final String INCONCLUSIVE_TEXT = ":inconclusive";

    private static String executeCommandOnFile(@NotNull final String command,
                                               @NotNull final String options,
                                               @NotNull final String filePath,
                                               final String cppcheckMisraPath) throws CppcheckError, ExecutionException {

        if (options.contains("--template")) {
            throw new CppcheckError("Cppcheck options contain --template field. " +
                    "Please remove this, the plugin defines its own.");
        }

        GeneralCommandLine cmd = new GeneralCommandLine()
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

        CapturingProcessHandler processHandler = new CapturingProcessHandler(cmd);
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        ProcessOutput output = processHandler.runProcessWithProgressIndicator(
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

    private static boolean isCFamilyFile(@NotNull final VirtualFile file) {
        final String fileExtension = file.getExtension();

        if (fileExtension == null) {
            return false;
        } else {
            final String lowerFileExtension = fileExtension.toLowerCase();
            return lowerFileExtension.equals("c") ||
                    lowerFileExtension.equals("cc") ||
                    lowerFileExtension.equals("cp") ||
                    lowerFileExtension.equals("cpp") ||
                    lowerFileExtension.equals("c++") ||
                    lowerFileExtension.equals("cxx") ||
                    lowerFileExtension.equals("h") ||
                    lowerFileExtension.equals("hh") ||
                    lowerFileExtension.equals("hpp");
        }
    }

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
}
