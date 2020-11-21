package com.github.johnthagen.cppcheck;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.FlowLayout;

class JFilePicker extends JPanel {
    private final JTextField textField;
    private final JFileChooser fileChooser;

    JFilePicker(String textFieldLabel, String buttonLabel) {
        fileChooser = new JFileChooser();

        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        final JLabel label = new JLabel(textFieldLabel);

        textField = new JTextField(30);
        final JButton button = new JButton(buttonLabel);

        button.addActionListener(evt -> buttonActionPerformed());

        add(label);
        add(textField);
        add(button);
    }

    JTextField getTextField() {
        return textField;
    }

    private void buttonActionPerformed() {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
}
