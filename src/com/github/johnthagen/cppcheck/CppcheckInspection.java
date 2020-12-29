package com.github.johnthagen.cppcheck;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.execution.ExecutionException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

class CppcheckInspection extends LocalInspectionTool {
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
            StatusBar.Info.set("[!] Error: Please set path of cppcheck in File->Settings->Cppcheck Configuration",
                    file.getProject());
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        String cppcheckOptions = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_OPTIONS);

        final String cppcheckMisraPath = Properties.get(Configuration.CONFIGURATION_KEY_CPPCHECK_MISRA_PATH);
        if (cppcheckMisraPath != null && !cppcheckMisraPath.isEmpty()) {
            cppcheckOptions = String.format("%s --addon=%s", cppcheckOptions, cppcheckMisraPath);
        }
        cppcheckOptions = String.format("%s --xml", cppcheckOptions);

        File tempFile = null;
        try {
            tempFile = FileUtil.createTempFile(RandomStringUtils.randomAlphanumeric(8) + "_", vFile.getName(), true);
            FileUtil.writeToFile(tempFile, document.getText());
            final String cppcheckOutput =
                    CppCheckInspectionImpl.executeCommandOnFile(cppcheckPath, prependIncludeDir(cppcheckOptions, vFile),
                            tempFile.getAbsolutePath(), cppcheckMisraPath);

            final List<ProblemDescriptor> descriptors = CppCheckInspectionImpl.parseOutput(file, manager, document, cppcheckOutput,
                    tempFile.getName());
            return descriptors.toArray(new ProblemDescriptor[0]);
        } catch (final ExecutionException | CppcheckError | IOException | SAXException | ParserConfigurationException ex) {
            Notifications.Bus.notify(new Notification("Cppcheck",
                    "Cppcheck execution failed.",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    NotificationType.ERROR));
        } finally {
            if (tempFile != null) {
                FileUtil.delete(tempFile);
            }
        }

        return ProblemDescriptor.EMPTY_ARRAY;
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
