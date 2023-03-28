package com.github.johnthagen.cppcheck;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class CppcheckInspection extends LocalInspectionTool {
    final static Path LATEST_RESULT_FILE = Paths.get(FileUtil.getTempDirectory(), "clion-cppcheck-latest.xml");

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile file,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        final VirtualFile vFile = file.getVirtualFile();
        if (vFile == null || !vFile.isInLocalFileSystem() || !SupportedExtensions.isCFamilyFile(vFile)) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        final Document document = FileDocumentManager.getInstance().getDocument(vFile);
        if (document == null || document.getLineCount() == 0) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        final String cppcheckPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_PATH);
        if (cppcheckPath == null || cppcheckPath.isEmpty()) {
            final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    file,
                    (TextRange)null,
                    "Please set 'Cppcheck Path' in the 'Cppcheck Configuration'.",
                    ProblemHighlightType.GENERIC_ERROR,
                    true);
            return new ProblemDescriptor[]{problemDescriptor};
        }

        final File cppcheckPathFile = new File(cppcheckPath);
        if (!cppcheckPathFile.exists()) {
            final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    file,
                    (TextRange)null,
                    "Configured 'Cppcheck Path' in the 'Cppcheck Configuration' does not exist: " + cppcheckPathFile.getAbsolutePath(),
                    ProblemHighlightType.GENERIC_ERROR,
                    true);
            return new ProblemDescriptor[]{problemDescriptor};
        }

        String cppcheckOptions = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_OPTIONS);

        final String cppcheckMisraPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_MISRA_PATH);
        if (cppcheckMisraPath != null && !cppcheckMisraPath.isEmpty()) {
            cppcheckOptions = String.format("%s --addon=%s", cppcheckOptions, cppcheckMisraPath);
        }
        cppcheckOptions = String.format("%s --xml", cppcheckOptions);

        ProblemDescriptor[] descriptors;
        File tempFile = null;
        try {
            tempFile = FileUtil.createTempFile(RandomStringUtils.randomAlphanumeric(8) + "_", vFile.getName(), true);
            FileUtil.writeToFile(tempFile, document.getText());
            final String cppcheckOutput =
                    CppCheckInspectionImpl.executeCommandOnFile(vFile, cppcheckPathFile, prependIncludeDir(cppcheckOptions, vFile),
                            tempFile, cppcheckMisraPath);

            // store the output of the latest analysis
            FileUtil.writeToFile(LATEST_RESULT_FILE.toFile(), cppcheckOutput);

            final List<ProblemDescriptor> descriptorsList = CppCheckInspectionImpl.parseOutput(file, manager, document, cppcheckOutput,
                    tempFile.getName());
            descriptors = descriptorsList.toArray(new ProblemDescriptor[0]);
        } catch (final ExecutionException | CppcheckError | IOException | SAXException | ParserConfigurationException ex) {
            CppcheckNotification.send("execution failed for " + vFile.getCanonicalPath(),
                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    NotificationType.ERROR);
            final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    file,
                    (TextRange)null,
                    "Cppcheck execution failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage().split("\n", 2)[0],
                    ProblemHighlightType.GENERIC_ERROR,
                    true);
            descriptors = new ProblemDescriptor[]{problemDescriptor};
        } finally {
            if (tempFile != null) {
                FileUtil.delete(tempFile);
            }
        }

        return descriptors;
    }

    @NotNull
    private static String prependIncludeDir(@NotNull final String cppcheckOptions, @NotNull final VirtualFile vFile) {
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
}
