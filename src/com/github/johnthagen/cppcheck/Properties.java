package com.github.johnthagen.cppcheck;

import com.intellij.ide.util.PropertiesComponent;

class Properties {
    private static final PropertiesComponent INSTANCE = PropertiesComponent.getInstance();

    static void set(String key, String value) {
        INSTANCE.setValue(key, value);
    }

    static String get(String key) {
        return INSTANCE.getValue(key);
    }
}
