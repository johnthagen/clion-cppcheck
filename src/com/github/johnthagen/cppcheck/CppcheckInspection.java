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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CppcheckInspection extends LocalInspectionTool {
    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file,
                                         @NotNull InspectionManager manager,
                                         boolean isOnTheFly) {
        final VirtualFile vFile = file.getVirtualFile();
        if (vFile == null || !isCFamilyFile(vFile)) {
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

        File tempFile = null;
        try {
            tempFile = FileUtil.createTempFile("", vFile.getName(), true);
            FileUtil.writeToFile(tempFile, document.getText());
            final String cppcheckOutput =
                    CppCheckInspectionImpl.executeCommandOnFile(cppcheckPath, prependIncludeDir(cppcheckOptions, vFile),
                            tempFile.getAbsolutePath(), cppcheckMisraPath);

            if (!cppcheckOutput.isEmpty()) {
                final List<ProblemDescriptor> descriptors = CppCheckInspectionImpl.parseOutput(file, manager, document, cppcheckOutput,
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

    private static boolean isCFamilyFile(@NotNull final VirtualFile file) {
        final String fileExtension = file.getExtension();
        if (fileExtension == null) {
            return false;
        }

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
