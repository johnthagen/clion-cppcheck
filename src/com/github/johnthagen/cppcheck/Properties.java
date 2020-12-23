package com.github.johnthagen.cppcheck;

import com.intellij.ide.util.PropertiesComponent;

class Properties {
    private static final PropertiesComponent INSTANCE = PropertiesComponent.getInstance();

    static void set(final String key, final String value) {
        INSTANCE.setValue(key, value);
    }

    static String get(final String key) {
        return INSTANCE.getValue(key);
    }
}
