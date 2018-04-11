package com.github.johnthagen.cppcheck;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Configuration implements Configurable {
  private boolean modified = false;
  private JFilePicker cppcheckFilePicker;
  private JTextField cppcheckOptionsField;
  private static final String CPPCHECK_NOTE =
    "Note: C++ projects should leave --language=c++ appended to the cppcheck options to avoid some " +
    "false positives in header files due to the fact that cppcheck implicitly defaults to " +
    "setting --language to \"c\" for .h files.";
  private CppcheckConfigurationModifiedListener
    listener = new CppcheckConfigurationModifiedListener(this);

  static final String CONFIGURATION_KEY_CPPCHECK_PATH = "cppcheck";
  static final String CONFIGURATION_KEY_CPPCHECK_OPTIONS = "cppcheckOptions";

  private static final String defaultOptions = "--enable=warning,performance,portability,style --language=c++";

  @Nls
  @Override
  public String getDisplayName() {
    return "cppcheck";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    JPanel jPanel = new JPanel();

    VerticalLayout verticalLayout = new VerticalLayout(1, 2);
    jPanel.setLayout(verticalLayout);

    cppcheckFilePicker = new JFilePicker("cppcheck path:", "...");
    JLabel optionFieldLabel = new JLabel("cppcheck options (default: " + defaultOptions + "):");
    cppcheckOptionsField = new JTextField(defaultOptions, 38);

    // The first time a user installs the plugin, save the default options in their properties.
    if (Properties.get(CONFIGURATION_KEY_CPPCHECK_OPTIONS) == null ||
        Properties.get(CONFIGURATION_KEY_CPPCHECK_OPTIONS).isEmpty()) {
      Properties.set(CONFIGURATION_KEY_CPPCHECK_OPTIONS, cppcheckOptionsField.getText());
    }

    JTextArea cppcheckNoteArea = new JTextArea(CPPCHECK_NOTE, 2, 80);
    cppcheckNoteArea.setLineWrap(true);
    cppcheckNoteArea.setWrapStyleWord(true);

    reset();

    cppcheckFilePicker.getTextField().getDocument().addDocumentListener(listener);
    cppcheckOptionsField.getDocument().addDocumentListener(listener);

    jPanel.add(cppcheckFilePicker);
    jPanel.add(optionFieldLabel);
    jPanel.add(cppcheckOptionsField);
    jPanel.add(cppcheckNoteArea);

    return jPanel;
  }

  @Override
  public boolean isModified() {
    return modified;
  }

  private void setModified() {
    this.modified = true;
  }

  @Override
  public void apply() {
    Properties.set(CONFIGURATION_KEY_CPPCHECK_PATH, cppcheckFilePicker.getTextField().getText());
    Properties.set(CONFIGURATION_KEY_CPPCHECK_OPTIONS, cppcheckOptionsField.getText());
    modified = false;
  }

  @Override
  public void reset() {
    String cppcheckPath = Properties.get(CONFIGURATION_KEY_CPPCHECK_PATH);
    cppcheckFilePicker.getTextField().setText(cppcheckPath);

    String cppcheckOptions = Properties.get(CONFIGURATION_KEY_CPPCHECK_OPTIONS);
    cppcheckOptionsField.setText(cppcheckOptions);

    modified = false;
  }

  @Override
  public void disposeUIResources() {
    cppcheckFilePicker.getTextField().getDocument().removeDocumentListener(listener);
    cppcheckOptionsField.getDocument().removeDocumentListener(listener);
  }

  private static class CppcheckConfigurationModifiedListener implements DocumentListener {
    private final Configuration option;

    CppcheckConfigurationModifiedListener(Configuration option) {
      this.option = option;
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
      option.setModified();
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
      option.setModified();
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
      option.setModified();
    }
  }
}