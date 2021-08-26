// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.uncrustify.settings;

import com.intellij.execution.ExecutionException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;
import org.jetbrains.uncrustify.ui.DocumentCheckerComponent;
import org.jetbrains.uncrustify.util.UncrustifyConfigFile;
import org.jetbrains.uncrustify.util.UncrustifyExecutable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

public class UncrustifySettingsComponent {
    private static final Logger log = Logger.getInstance(UncrustifySettingsComponent.class);

    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myExecutablePath = new TextFieldWithBrowseButton();
    private final VersionCheckerComponent myVersionCheckField = new VersionCheckerComponent(myExecutablePath.getTextField().getDocument());
    private final TextFieldWithBrowseButton myConfigPath = new TextFieldWithBrowseButton();

    public UncrustifySettingsComponent(@Nullable Project project) {
        myMainPanel = new JPanel(new GridBagLayout());
        GridBag bag = new GridBag()
                .setDefaultWeightX(1, 1.0)
                .setDefaultAnchor(GridBagConstraints.CENTER)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultInsets(0, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP, 0, UIUtil.DEFAULT_HGAP)
                .setDefaultInsets(UIUtil.DEFAULT_VGAP, 0, 0, 0);
        myMainPanel.add(new JBLabel(UncrustifyBundle.message("uncrustify.settings.executablePath.label")), bag.nextLine().next());
        myMainPanel.add(myExecutablePath, bag.next().fillCell());
        myMainPanel.add(myVersionCheckField, bag.nextLine().next().next().insets(0, -1, -1, -1).fillCell());
        myMainPanel.add(new JBLabel(UncrustifyBundle.message("uncrustify.settings.configPath.label")), bag.nextLine().next());
        myMainPanel.add(myConfigPath, bag.next().fillCell());

        myMainPanel.add(Box.createVerticalGlue(), bag.nextLine().next().weighty(1.0).fillCell());

        myExecutablePath.addBrowseFolderListener(
                UncrustifyBundle.message("uncrustify.settings.executablePath.title"),
                UncrustifyBundle.message("uncrustify.settings.executablePath.description"),
                project,
                FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
        );

        myConfigPath.addBrowseFolderListener(
                UncrustifyBundle.message("uncrustify.settings.configPath.title"),
                UncrustifyBundle.message("uncrustify.settings.configPath.description"),
                project,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
        );

        myVersionCheckField.checkDocument();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myExecutablePath;
    }

    @NotNull
    public String getExecutablePath() {
        return myExecutablePath.getText();
    }

    public void setExecutablePath(@NotNull String newText) {
        myExecutablePath.setText(newText);
    }

    public boolean isExecutablePathValid() {
        return myVersionCheckField.isPathValid();
    }

    public @NotNull String getConfigPath() {
        return myConfigPath.getText();
    }

    public void setConfigPath(@NotNull String text) {
        myConfigPath.setText(text);
    }

    private static class VersionCheckerComponent extends DocumentCheckerComponent {

        public VersionCheckerComponent(@NotNull Document document) {
            super(document);
        }

        @Override
        public void setPathIsEmpty(String message) {
            super.setPathIsEmpty(message);
            clear();
            setIcon(AllIcons.General.Warning);
            append(UncrustifyBundle.message("uncrustify.settings.executableStatus.none"));
        }

        @Override
        public void setPathIsValid(String message) {
            super.setPathIsEmpty(message);
            clear();
            setIcon(AllIcons.General.InspectionsOK);
            append(message);
        }

        @Override
        public void setPathIsInvalid(String message) {
            super.setPathIsInvalid(message);
            clear();
            setIcon(AllIcons.General.Error);
            append(message);
        }

        public void checkDocument() {
            try {
                if (getDocument().getLength() <= 0) {
                    setPathIsEmpty(null);
                } else {
                    setPathIsBeingChecked(null);
                    UncrustifyExecutable.verify(
                            getDocument().getText(0, getDocument().getLength()),
                            new UncrustifyExecutable.VerificationListener() {
                                @Override
                                public void onInvalid() {
                                    setPathIsInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notUncrustify"));
                                }

                                @Override
                                public void onValid(String version) {
                                    setPathIsValid(version);
                                }
                            },
                            false);
                }
            } catch (BadLocationException ex) {
                log.error(ex);
            } catch (ExecutionException ex) {
                setPathIsInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notExecutable"));
            }
        }
    }
}
