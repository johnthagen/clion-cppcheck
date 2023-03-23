package com.github.johnthagen.cppcheck;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.github.johnthagen.cppcheck.CppcheckInspection.LATEST_RESULT_FILE;

public class ShowOutputAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
        final VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(LATEST_RESULT_FILE.toFile());
        if (vFile == null) {
            return;
        }

        final Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }

        // TODO: make it possible to view the output for the currently open file
        // TODO: find a way to display this without writing to the file system
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, vFile), true);
    }
}
