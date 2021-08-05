// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.uncrustify.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uncrustify.UncrustifyBundle;

import javax.swing.*;

public class UncrustifySettingsComponent {

    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myUncrustifyExecutablePath = new TextFieldWithBrowseButton();
    private final JBCheckBox myUncrustifyFormattingEnabled = new JBCheckBox(UncrustifyBundle.message("uncrustify.settings.enableFormatting.label"));

    public UncrustifySettingsComponent(@NotNull Project project) {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(
                        UncrustifyBundle.message("uncrustify.settings.executablePath.label")),
                        myUncrustifyExecutablePath,
                        1,
                        false)
                .addComponent(myUncrustifyFormattingEnabled, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        myUncrustifyExecutablePath.addBrowseFolderListener(
                UncrustifyBundle.message("uncrustify.settings.executablePath.title"),
                UncrustifyBundle.message("uncrustify.settings.executablePath.description"),
                project,
                FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
        );
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myUncrustifyExecutablePath;
    }

    @NotNull
    public String getUncrustifyExecutablePath() {
        return myUncrustifyExecutablePath.getText();
    }

    public void setUncrustifyExecutablePath(@NotNull String newText) {
        myUncrustifyExecutablePath.setText(newText);
    }

    public boolean getUncrustifyFormattingEnabled() {
        return myUncrustifyFormattingEnabled.isSelected();
    }

    public void setUncrustifyFormattingEnabled(boolean newStatus) {
        myUncrustifyFormattingEnabled.setSelected(newStatus);
    }

}
