package com.github.johnthagen.cppcheck;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class Configuration implements Configurable {
    private boolean modified = false;
    private JFilePicker cppcheckFilePicker;
    private JTextField cppcheckOptionsField;
    private JFilePicker cppcheckMisraFilePicker;
    private static final String CPPCHECK_NOTE =
            "Note: C++ projects should leave --language=c++ appended to the Cppcheck options to avoid some " +
                    "false positives in header files due to the fact that Cppcheck implicitly defaults to " +
                    "setting --language to \"c\" for .h files.";
    private static final String CPPCHECK_MISRA_NOTE =
            "Using MISRA requires a rule texts file, which can be obtained from MISRA themselves " +
                    "(Their license prohibits distributing the rules texts)\n\n" +
                    "Create a .json file near your Cppcheck installation and point to it here\n" +
                    "Within that file, create something like this:\n" +
                    "{\n" +
                    "    \"script\": \"misra.py\",\n" +
                    "    \"args\": [\"--rule-texts=<Path To MISRA Rules.txt>\"]\n" +
                    "}";

    private final CppcheckConfigurationModifiedListener
            listener = new CppcheckConfigurationModifiedListener(this);

    static final String CONFIGURATION_KEY_CPPCHECK_PATH = "cppcheck";
    static final String CONFIGURATION_KEY_CPPCHECK_OPTIONS = "cppcheckOptions";
    static final String CONFIGURATION_KEY_CPPCHECK_MISRA_PATH = "cppcheckMisraPath";

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
        final JPanel jPanel = new JPanel();

        final VerticalLayout verticalLayout = new VerticalLayout(1, 2);
        jPanel.setLayout(verticalLayout);

        cppcheckFilePicker = new JFilePicker("Cppcheck Path:", "...");
        final JLabel optionFieldLabel = new JLabel("Cppcheck Options (Default: " + defaultOptions + "):");
        cppcheckOptionsField = new JTextField(defaultOptions, 38);
        cppcheckMisraFilePicker = new JFilePicker("MISRA Addon JSON:", "...");

        // The first time a user installs the plugin, save the default options in their properties.
        if (Properties.get(CONFIGURATION_KEY_CPPCHECK_OPTIONS) == null ||
                Properties.get(CONFIGURATION_KEY_CPPCHECK_OPTIONS).isEmpty()) {
            Properties.set(CONFIGURATION_KEY_CPPCHECK_OPTIONS, cppcheckOptionsField.getText());
        }

        if (Properties.get(CONFIGURATION_KEY_CPPCHECK_MISRA_PATH) == null) {
            cppcheckMisraFilePicker.getTextField().setText("");
            Properties.set(CONFIGURATION_KEY_CPPCHECK_MISRA_PATH, cppcheckMisraFilePicker.getTextField().getText());
        }

        final JTextArea cppcheckNoteArea = new JTextArea(CPPCHECK_NOTE, 2, 80);
        cppcheckNoteArea.setLineWrap(true);
        cppcheckNoteArea.setWrapStyleWord(true);

        final JTextArea cppcheckMisraNoteArea = new JTextArea(CPPCHECK_MISRA_NOTE, 2, 80);
        cppcheckMisraNoteArea.setLineWrap(true);
        cppcheckMisraNoteArea.setWrapStyleWord(true);

        reset();

        cppcheckFilePicker.getTextField().getDocument().addDocumentListener(listener);
        cppcheckOptionsField.getDocument().addDocumentListener(listener);
        cppcheckMisraFilePicker.getTextField().getDocument().addDocumentListener(listener);

        jPanel.add(cppcheckFilePicker);
        jPanel.add(optionFieldLabel);
        jPanel.add(cppcheckOptionsField);
        jPanel.add(cppcheckNoteArea);

        jPanel.add(cppcheckMisraFilePicker);
        jPanel.add(cppcheckMisraNoteArea);
        return jPanel;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    private void setModified() {
        modified = true;
    }

    @Override
    public void apply() {
        Properties.set(CONFIGURATION_KEY_CPPCHECK_PATH, cppcheckFilePicker.getTextField().getText());
        Properties.set(CONFIGURATION_KEY_CPPCHECK_OPTIONS, cppcheckOptionsField.getText());
        Properties.set(CONFIGURATION_KEY_CPPCHECK_MISRA_PATH, cppcheckMisraFilePicker.getTextField().getText());
        modified = false;
    }

    @Override
    public void reset() {
        final String cppcheckPath = Properties.get(CONFIGURATION_KEY_CPPCHECK_PATH);
        cppcheckFilePicker.getTextField().setText(cppcheckPath);

        final String cppcheckOptions = Properties.get(CONFIGURATION_KEY_CPPCHECK_OPTIONS);
        cppcheckOptionsField.setText(cppcheckOptions);

        final String cppcheckMisraPath = Properties.get(CONFIGURATION_KEY_CPPCHECK_MISRA_PATH);
        cppcheckMisraFilePicker.getTextField().setText(cppcheckMisraPath);

        modified = false;
    }

    @Override
    public void disposeUIResources() {
        cppcheckFilePicker.getTextField().getDocument().removeDocumentListener(listener);
        cppcheckOptionsField.getDocument().removeDocumentListener(listener);
        cppcheckMisraFilePicker.getTextField().getDocument().removeDocumentListener(listener);
    }

    private static class CppcheckConfigurationModifiedListener implements DocumentListener {
        private final Configuration option;

        CppcheckConfigurationModifiedListener(final Configuration option) {
            this.option = option;
        }

        @Override
        public void insertUpdate(final DocumentEvent documentEvent) {
            option.setModified();
        }

        @Override
        public void removeUpdate(final DocumentEvent documentEvent) {
            option.setModified();
        }

        @Override
        public void changedUpdate(final DocumentEvent documentEvent) {
            option.setModified();
        }
    }
}