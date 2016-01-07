package com.github.johnthagen.cppcheck;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Configuration implements Configurable {
  private boolean modified = false;
  private JFilePicker cppcheckFilePicker;
  private JTextField cppcheckOptionsField;
  private CppcheckConfigurationModifiedListener
    listener = new CppcheckConfigurationModifiedListener(this);

  public static final String CONFIGURATION_KEY_CPPCHECK_PATH = "cppcheck";
  public static final String CONFIGURATION_KEY_CPPCHECK_OPTIONS = "cppcheckOptions";

  public static final String defaultOptions = "--enable=warning,performance,portability,style";

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

    reset();

    cppcheckFilePicker.getTextField().getDocument().addDocumentListener(listener);
    cppcheckOptionsField.getDocument().addDocumentListener(listener);

    jPanel.add(cppcheckFilePicker);
    jPanel.add(optionFieldLabel);
    jPanel.add(cppcheckOptionsField);

    return jPanel;
  }

  @Override
  public boolean isModified() {
    return modified;
  }

  public void setModified(boolean modified) {
    this.modified = modified;
  }

  @Override
  public void apply() throws ConfigurationException {
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

    public CppcheckConfigurationModifiedListener(Configuration option) {
      this.option = option;
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
      option.setModified(true);
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
      option.setModified(true);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
      option.setModified(true);
    }
  }
}