package com.github.johnthagen.cppcheck;

import org.jetbrains.annotations.NotNull;

public class CppcheckError extends Error {
    public CppcheckError(@NotNull final String message)
    {
        super(message);
    }
}
