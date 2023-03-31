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
import java.util.ArrayList;
import java.util.List;

class CppcheckInspection extends LocalInspectionTool {
    final static Path LATEST_RESULT_FILE = Paths.get(FileUtil.getTempDirectory(), "clion-cppcheck-latest.xml");

    private static ProblemDescriptor createProblemDescriptor(@NotNull final PsiFile file,
                                                             @NotNull final InspectionManager manager,
                                                             @NotNull final String msg) {
        return manager.createProblemDescriptor(
                file,
                (TextRange)null,
                msg,
                ProblemHighlightType.GENERIC_ERROR,
                true);
    }

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
            final ProblemDescriptor problemDescriptor = createProblemDescriptor(file, manager, "Please set 'Cppcheck Path' in 'Cppcheck Configuration'.");
            return new ProblemDescriptor[]{problemDescriptor};
        }

        final File cppcheckPathFile = new File(cppcheckPath);
        if (!cppcheckPathFile.exists()) {
            final ProblemDescriptor problemDescriptor = createProblemDescriptor(file, manager, "Configured 'Cppcheck Path' in 'Cppcheck Configuration' does not exist: " + cppcheckPathFile.getAbsolutePath());
            return new ProblemDescriptor[]{problemDescriptor};
        }

        final ArrayList<ProblemDescriptor> descriptors = new ArrayList<>();

        String cppcheckOptions = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_OPTIONS);

        final String cppcheckMisraPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_MISRA_PATH);
        if (cppcheckMisraPath != null && !cppcheckMisraPath.isEmpty()) {
            final File cppcheckMisraPathFile = new File(cppcheckMisraPath);
            if (!cppcheckMisraPathFile.exists()) {
                final ProblemDescriptor problemDescriptor = createProblemDescriptor(file, manager, "Configured 'MISRA Addon JSON' in 'Cppcheck Configuration' does not exist: " + cppcheckMisraPathFile.getAbsolutePath());
                descriptors.add(problemDescriptor);
            }
            else {
                cppcheckOptions = String.format("%s --addon=%s", cppcheckOptions, cppcheckMisraPath);
            }
        }
        cppcheckOptions = String.format("%s --xml", cppcheckOptions);

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
            descriptors.addAll(descriptorsList);
        } catch (final ExecutionException | CppcheckError | IOException | SAXException | ParserConfigurationException ex) {
            CppcheckNotification.send("execution failed for " + vFile.getCanonicalPath(),
                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    NotificationType.ERROR);
            final ProblemDescriptor problemDescriptor = createProblemDescriptor(file, manager, "Cppcheck execution failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage().split("\n", 2)[0]);
            descriptors.add(problemDescriptor);
        } finally {
            if (tempFile != null) {
                FileUtil.delete(tempFile);
            }
        }

        return descriptors.toArray(new ProblemDescriptor[0]);
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
