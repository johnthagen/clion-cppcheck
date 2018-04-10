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
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiFile;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    String cppcheckPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_PATH);
    String cppcheckOptions = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_OPTIONS);

    if (!isCFamilyFile(file)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    if (cppcheckPath == null || cppcheckPath.isEmpty()) {
      StatusBar.Info.set("[!] Error: Please set path of cppcheck in File->Settings->Other Settings",
                         file.getProject());
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
    if (document == null || document.getLineCount() == 0) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    try {
      String cppcheckOutput = executeCommandOnFile(cppcheckPath, cppcheckOptions, file);

      if (!cppcheckOutput.isEmpty()) {
        List<ProblemDescriptor> descriptors = parseOutput(file, manager, document, cppcheckOutput);
        return descriptors.toArray(new ProblemDescriptor[0]);
      }
    } catch (ExecutionException ex) {
      Notifications.Bus.notify(new Notification("cppcheck",
                                                "Error",
                                                "ExecutionException: " + ex.getMessage(),
                                                NotificationType.INFORMATION));
      ex.printStackTrace();
    }

    return ProblemDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  private List<ProblemDescriptor> parseOutput(@NotNull PsiFile file, @NotNull InspectionManager manager, Document document, String cppcheckOutput) {
    List<ProblemDescriptor> descriptors = new ArrayList<>();
    Scanner scanner = new Scanner(cppcheckOutput);

    //Notifications.Bus.notify(new Notification("cppcheck",
    //                                          "Info",
    //                                          file.getVirtualFile().getCanonicalPath() + "\n" +
    //                                          cppcheckOutput,
    //                                          NotificationType.INFORMATION));

    // Example output line:
    // [C:\Users\John Hagen\ClionProjects\test\main.cpp:12]: (style) Variable 'a' is not assigned a value.
    // [main.cpp:12] -> [main.cpp:14]: (performance) Variable 'a' is reassigned a value before the old one has been used.
    Pattern pattern = Pattern.compile("^\\[(.+?):(\\d+)\\](?:\\s+->\\s+\\[.+\\])?:\\s+\\((\\w+)\\)\\s+(.+)");

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
      if (!fileName.equals(file.getName())) {
        continue;
      }

      // Because cppcheck runs on physical files, it's possible for the editor lines
      // (lines in the IDE memory) to get out of sync from the lines on disk.
      if (lineNumber > document.getLineCount()) {
        lineNumber = document.getLineCount();
      }

      // Cppcheck error or parsing error.
      if (lineNumber <= 0) {
        continue;
      }

      // Document counts lines starting at 0, rather than 1 like in cppcheck.
      lineNumber -= 1;

      final int lineStartOffset = document.getLineStartOffset(lineNumber);
      final int lintEndOffset = document.getLineEndOffset(lineNumber);

      // Do not highlight empty whitespace prepended to lines.
      String line_text = document.getImmutableCharSequence().subSequence(
        lineStartOffset, lintEndOffset).toString();

      final int numberOfPrependedSpaces = line_text.length() -
                                          line_text.replaceAll("^\\s+","").length();

      ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
        file,
        TextRange.create(lineStartOffset + numberOfPrependedSpaces, lintEndOffset),
        "cppcheck: (" + severity + ") " + errorMessage,
        severityToHighlightType(severity),
        true);
      descriptors.add(problemDescriptor);
    }
    return descriptors;
  }

  private static final int TIMEOUT_MS = 60 * 1000;

  private static String executeCommandOnFile(final String command,
                                             final String options,
                                             @NotNull final PsiFile file) throws ExecutionException {
    GeneralCommandLine cmd = new GeneralCommandLine()
      .withExePath(command)
      .withParameters(ParametersListUtil.parse(options))
      .withParameters("\"" + file.getVirtualFile().getCanonicalPath() + "\"");
    CapturingProcessHandler processHandler = new CapturingProcessHandler(cmd);
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    ProcessOutput output = processHandler.runProcessWithProgressIndicator(
      indicator != null ? indicator : new EmptyProgressIndicator(),
      TIMEOUT_MS);

    if (output.isCancelled()) {
      throw new ProcessCanceledException();
    }

    if (output.isTimeout()) {
      throw new ExecutionException(command + " has timed out");
    }

    if (output.getExitCode() != 0) {
      throw new ExecutionException(command + " has finished with exit code " + output.getExitCode());
    }

    return output.getStderr();
  }

  private static boolean isCFamilyFile(@NotNull final PsiFile file) {
    final String fileExtension = file.getVirtualFile().getExtension();

    if (fileExtension == null) {
      return false;
    }
    else {
      final String lowerFileExtension = fileExtension.toLowerCase();
      if (lowerFileExtension.equals("c") ||
          lowerFileExtension.equals("cc") ||
          lowerFileExtension.equals("cp") ||
          lowerFileExtension.equals("cpp") ||
          lowerFileExtension.equals("c++") ||
          lowerFileExtension.equals("cxx") ||
          lowerFileExtension.equals("h") ||
          lowerFileExtension.equals("hh") ||
          lowerFileExtension.equals("hpp")) {
        return true;
      }
      else {
        return false;
      }
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
