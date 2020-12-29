package com.github.johnthagen.cppcheck;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class SupportedExtensions {
    // TODO: get the list of supported extensions from Cppcheck if it provides that information
    // TODO: extend list by extensions configured within CLion
    private final static List<String> supportedCExtensions = new ArrayList<>(Arrays.asList(
            "c",
            "cl"));

    private final static List<String> supportedCPPExtensions = new ArrayList<>(Arrays.asList(
            "cc",
            "cp",
            "cpp",
            "c++",
            "cxx",
            "hh",
            "hpp",
            "hxx",
            "tpp",
            "txx"));

    private final static List<String> supportedHeaderExtensions = new ArrayList<>(Collections.singletonList(
            "h"));

    public static boolean isCFamilyFile(@NotNull final VirtualFile file) {
        return isCFile(file) || isCPPFile(file) || isHeaderFile(file);
    }

    private static boolean isFile(@NotNull final VirtualFile file, @NotNull final List<String> supportedExtensions)
    {
        final String fileExtension = file.getExtension();
        if (fileExtension == null) {
            return false;
        }

        final String lowerFileExtension = fileExtension.toLowerCase();
        return supportedExtensions.contains(lowerFileExtension);
    }

    public static boolean isCFile(@NotNull final VirtualFile file) {
        return isFile(file, supportedCExtensions);
    }

    public static boolean isCPPFile(@NotNull final VirtualFile file) {
        return isFile(file, supportedCPPExtensions);
    }

    public static boolean isHeaderFile(@NotNull final VirtualFile file) {
        return isFile(file, supportedHeaderExtensions);
    }
}
