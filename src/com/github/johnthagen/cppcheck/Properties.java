package com.github.johnthagen.cppcheck;

import com.intellij.ide.util.PropertiesComponent;

public class Properties {
  private static final PropertiesComponent INSTANCE = PropertiesComponent.getInstance();

  public static void set(String key, String value) {
    INSTANCE.setValue(key, value);
  }

  public static String get(String key) {
    return INSTANCE.getValue(key);
  }
}
