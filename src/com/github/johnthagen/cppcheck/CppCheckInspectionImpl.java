package com.github.johnthagen.cppcheck;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

class CppCheckInspectionImpl {
    private static ProblemHighlightType severityToHighlightType(@NotNull final String severity) {
        switch (severity) {
            case "error":
                return ProblemHighlightType.GENERIC_ERROR;
            case "warning":
                return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
            case "style":
            case "performance":
            case "portability":
            case "debug":
                return ProblemHighlightType.WEAK_WARNING;
            case "information":
                return ProblemHighlightType.INFORMATION;

            // If the severity is not understood (changes in Cppcheck), return ERROR.
            default:
                return ProblemHighlightType.ERROR;
        }
    }

    // TODO: make configurable
    private static final boolean VERBOSE_LOG = false;
    private static final String INCONCLUSIVE_TEXT = ":inconclusive";

    @NotNull
    public static List<ProblemDescriptor> parseOutput(@NotNull final PsiFile psiFile,
                                                      @NotNull final InspectionManager manager,
                                                      @NotNull final Document document,
                                                      @NotNull final String cppcheckOutput,
                                                      @NotNull final String sourceFileName) throws IOException, SAXException, ParserConfigurationException {

        if (VERBOSE_LOG) {
            // TODO: provide XML output via a "Show Cppcheck output" action - event log messages are truncated
            CppcheckNotification.send("execution output for " + psiFile.getVirtualFile().getCanonicalPath(),
                    cppcheckOutput,
                    NotificationType.INFORMATION);
        }

        final List<ProblemDescriptor> descriptors = new ArrayList<>();

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final org.w3c.dom.Document doc = db.parse(new InputSource(new StringReader(cppcheckOutput)));

        final NodeList errors = doc.getElementsByTagName("error");
        for (int i = 0; i < errors.getLength(); ++i) {
            /*
                <error id="accessMoved" severity="warning" msg="Access of moved variable &apos;a&apos;." verbose="Access of moved variable &apos;a&apos;." cwe="672" hash="6576707224072251515" inconclusive="true">
                    <location file="/mnt/s/clion/example_lite_2/test.cpp" line="14" column="18" info="Access of moved variable &apos;a&apos;."/>
                    <location file="/mnt/s/clion/example_lite_2/test.cpp" line="13" column="7" info="Calling std::move(a)"/>
                    <symbol>a</symbol>
                </error>
            */

            final Node error = errors.item(i);
            final NamedNodeMap attributes = error.getAttributes();

            final String id = attributes.getNamedItem("id").getNodeValue();

            // Skip the "toomanyconfigs" error
            /*
       			<error id="toomanyconfigs" severity="information" msg="Too many #ifdef configurations - cppcheck only checks 1 of 12 configurations. Use --force to check all configurations." verbose="The checking of the file will be interrupted because there are too many #ifdef configurations. Checking of all #ifdef configurations can be forced by --force command line option or from GUI preferences. However that may increase the checking time." cwe="398">
                    <location file="C:\Users\Name\AppData\Local\Temp\___valueflow.cpp" line="0" column="0"/>
                </error>
            */
            if (id.equals("toomanyconfigs")) {
                continue;
            }

            // Skip the "missingIncludeSystem" error
            /*
       			<error id="missingIncludeSystem" severity="information" msg="Cppcheck cannot find all the include files (use --check-config for details)" verbose="Cppcheck cannot find all the include files. Cppcheck can check the code without the include files found. But the results will probably be more accurate if all the include files are found. Please check your project&apos;s include directories and add all of them as include directories for Cppcheck. To see what files Cppcheck cannot find use --check-config."/>
            */
            if (id.equals("missingIncludeSystem")) {
                continue;
            }

            // suppress this warning until Cppcheck handles them in a better way
            if (id.equals("unusedFunction")) {
                continue;
            }

            // suppress this warning for headers until Cppcheck handles them in a better way
            if (SupportedExtensions.isHeaderFile(psiFile.getVirtualFile()) && id.equals("unusedStructMember")) {
                continue;
            }

            final String severity = attributes.getNamedItem("severity").getNodeValue();
            final String errorMessage = attributes.getNamedItem("msg").getNodeValue();
            final Node inconclusiveNode = attributes.getNamedItem("inconclusive");
            final boolean inconclusive = inconclusiveNode != null && inconclusiveNode.getNodeValue().equals("true");

            Node location = null;

            // look for the first "location" child name
            final NodeList children = error.getChildNodes();
            for (int j = 0; j < children.getLength(); ++j) {
                final Node child = children.item(j);
                if (child.getNodeName().equals("location")) {
                    location = child;
                    break;
                }
            }

            // ignore entries without location e.g. missingIncludeSystem
            if (location == null) {
                continue;
            }

            final NamedNodeMap locationAttributes = location.getAttributes();
            final String fileName = new File(locationAttributes.getNamedItem("file").getNodeValue()).getName();
            int lineNumber = Integer.parseInt(locationAttributes.getNamedItem("line").getNodeValue());
            final int column = Integer.parseInt(locationAttributes.getNamedItem("column").getNodeValue()); // TODO

            // If a file #include's header files, Cppcheck will also run on the header files and print
            // any errors. These errors don't apply to the current file and should not be drawn. They can
            // be distinguished by checking the file name.
            if (!fileName.equals(sourceFileName)) {
                continue;
            }

            // Cppcheck error
            if (lineNumber <= 0 || lineNumber > document.getLineCount()) {
                CppcheckNotification.send("line number out-of-bounds for " + psiFile.getVirtualFile().getCanonicalPath(),
                        id + " " + severity + " " + inconclusive + " " + errorMessage + " " + fileName + " " + lineNumber + " " + column,
                        NotificationType.ERROR);
                continue;
            }

            // Document counts lines starting at 0, rather than 1 like in cppcheck.
            lineNumber -= 1;

            final int lineStartOffset = DocumentUtil.getFirstNonSpaceCharOffset(document, lineNumber);
            final int lineEndOffset = document.getLineEndOffset(lineNumber);

            final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    psiFile,
                    TextRange.create(lineStartOffset, lineEndOffset),
                    "Cppcheck: (" + severity + (inconclusive ? INCONCLUSIVE_TEXT : "") + ") " + id + ": " + errorMessage,
                    severityToHighlightType(severity),
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
        final GeneralCommandLine cmd = new GeneralCommandLine()
                .withExePath(command)
                .withParameters(ParametersListUtil.parse(options))
                .withParameters(ParametersListUtil.parse("\"" + filePath + "\""));

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
                        "stdout: " + output.getStdout() + "\n" +
                        "stderr: " + output.getStderr());
            }
        }

        return output.getStderr();
    }
}
