// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.uncrustify.settings;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessAdapter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;
import org.jetbrains.uncrustify.UncrustifyUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class UncrustifySettingsComponent {
    private static final Logger log = Logger.getInstance(UncrustifySettingsComponent.class);

    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myUncrustifyExecutablePath = new TextFieldWithBrowseButton();
    private final VersionCheckerComponent myVersionField = new VersionCheckerComponent(myUncrustifyExecutablePath.getTextField().getDocument());

    public UncrustifySettingsComponent(@Nullable Project project) {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(
                        UncrustifyBundle.message("uncrustify.settings.executablePath.label")),
                        myUncrustifyExecutablePath,
                        1,
                        false)
                .addComponent(myVersionField, -2)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        myUncrustifyExecutablePath.addBrowseFolderListener(
                UncrustifyBundle.message("uncrustify.settings.executablePath.title"),
                UncrustifyBundle.message("uncrustify.settings.executablePath.description"),
                project,
                FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
        );

        myVersionField.checkVersion();
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

    public boolean isUncrustifyExecutablePathValid() {
        return myVersionField.isPathValid();
    }

    public void setUncrustifyExecutablePath(@NotNull String newText) {
        myUncrustifyExecutablePath.setText(newText);
    }

    private static class VersionCheckerComponent extends SimpleColoredComponent {
        public static final String EXECUTABLE_VALID_PROPERTY = "uncrustifyVersionValid";

        private boolean myCheckedPathValid;
        private final Document myCheckedDocument;

        public VersionCheckerComponent(@NotNull Document document) {
            super();
            myCheckedDocument = document;
            document.addDocumentListener(new PathToExecutableAdapter());
            checkVersion();
        }

        private void setValid(boolean b) {
            boolean changed = myCheckedPathValid != b;
            myCheckedPathValid = b;
            if (changed) {
                firePropertyChange(EXECUTABLE_VALID_PROPERTY, !myCheckedPathValid, myCheckedPathValid);
            }
        }

        public boolean isPathValid() {
            return myCheckedPathValid;
        }

        public void setPathIsEmpty() {
            clear();
            setIcon(AllIcons.General.Warning);
            append(UncrustifyBundle.message("uncrustify.settings.executableStatus.none"));
            setValid(false);
        }

        public void setPathIsValid(String version) {
            clear();
            setIcon(AllIcons.General.InspectionsOK);
            append(version);
            setValid(true);
        }

        public void setPathIsInvalid(String message) {
            clear();
            setIcon(AllIcons.General.Error);
            append(message);
            setValid(false);
        }

        public void setPathIsBeingChecked() {
            clear();
            setIcon(AnimatedIcon.Default.INSTANCE);
            setValid(false);
        }

        public void checkVersion() {
            try {
                if (myCheckedDocument.getLength() <= 0) {
                    setPathIsEmpty();
                } else {
                    OSProcessHandler handler = UncrustifyUtil.createProcessHandler(
                            myCheckedDocument.getText(0, myCheckedDocument.getLength()),
                            "--version");
                    handler.addProcessListener(new CapturingProcessAdapter() {
                        @Override
                        public void startNotified(@NotNull ProcessEvent event) {
                            super.startNotified(event);
                            setPathIsBeingChecked();
                        }

                        public void processTerminated(@NotNull ProcessEvent event) {
                            super.processTerminated(event);

                            int exitCode = event.getExitCode();
                            ProcessOutput processOutput = getOutput();
                            if (exitCode == 0) {
                                String version = UncrustifyUtil.validateVersion(processOutput.getStdout());
                                if (version != null) {
                                    setPathIsValid(version);
                                } else {
                                    setPathIsInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notUncrustify"));
                                }
                            } else {
                                setPathIsInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notUncrustify"));
                            }
                        }
                    });
                    //TODO after some timeout, kill the process (for cases when it doesn't terminate on its own for some reason)
                    handler.startNotify();
                }
            } catch (BadLocationException ex) {
                log.error(ex);
            } catch (ExecutionException ex) {
                setPathIsInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notExecutable"));
            }
        }

        public class PathToExecutableAdapter extends DocumentAdapter {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                checkVersion();
            }
        }
    }
}
