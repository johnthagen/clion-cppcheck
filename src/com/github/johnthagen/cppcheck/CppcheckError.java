package com.github.johnthagen.cppcheck;

import org.jetbrains.annotations.NotNull;

class CppcheckError extends Error {
    public CppcheckError(@NotNull final String message)
    {
        super(message);
    }
}
