// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.uncrustify.settings;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
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
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uncrustify.UncrustifyBundle;
import org.jetbrains.uncrustify.UncrustifyUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.nio.charset.StandardCharsets;

public class UncrustifySettingsComponent {
    private static final Logger log = Logger.getInstance(UncrustifySettingsComponent.class);

    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myUncrustifyExecutablePath = new TextFieldWithBrowseButton();
    private final JBCheckBox myUncrustifyFormattingEnabled = new JBCheckBox(UncrustifyBundle.message("uncrustify.settings.enableFormatting.label"));
    private final VersionFieldComponent myVersionField = new VersionFieldComponent(myUncrustifyExecutablePath.getTextField().getDocument());

    public UncrustifySettingsComponent(@NotNull Project project) {
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(
                        UncrustifyBundle.message("uncrustify.settings.executablePath.label")),
                        myUncrustifyExecutablePath,
                        1,
                        false)
                .addComponent(myVersionField)
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

    private static class VersionFieldComponent extends SimpleColoredComponent {
        public VersionFieldComponent(@NotNull Document document) {
            super();
            setPathEmpty();
            document.addDocumentListener(new PathToExecutableAdapter());
        }

        public void setPathEmpty() {
            clear();
            setIcon(AllIcons.General.Warning);
            append(UncrustifyBundle.message("uncrustify.settings.executableStatus.none"));
        }

        public void setPathValid(String version) {
            clear();
            setIcon(AllIcons.General.InspectionsOK);
            append(version);
        }

        public void setPathInvalid(String message) {
            clear();
            setIcon(AllIcons.General.Error);
            append(message);
        }

        public void setCheckingPath() {
            clear();
            setIcon(AnimatedIcon.Default.INSTANCE);
        }

        public class PathToExecutableAdapter extends DocumentAdapter {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                try {
                    Document document = e.getDocument();
                    if (document.getLength() <= 0) {
                        setPathEmpty();
                    } else {
                        GeneralCommandLine commandLine = new GeneralCommandLine()
                                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                                .withExePath(e.getDocument().getText(0, e.getDocument().getLength()))
                                .withParameters("--version");

                        OSProcessHandler handler = new OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8));
                        handler.addProcessListener(new CapturingProcessAdapter() {
                            @Override
                            public void startNotified(@NotNull ProcessEvent event) {
                                super.startNotified(event);
                                setCheckingPath();
                            }

                            public void processTerminated(@NotNull ProcessEvent event) {
                                super.processTerminated(event);

                                int exitCode = event.getExitCode();
                                ProcessOutput processOutput = getOutput();
                                if (exitCode == 0) {
                                    String version = UncrustifyUtil.validateVersion(processOutput.getStdout());
                                    if (version != null) {
                                        setPathValid(version);
                                    } else {
                                        setPathInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notUncrustify"));
                                    }
                                } else {
                                    setPathInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notUncrustify"));
                                }
                            }
                        });
                        handler.startNotify();
                    }
                } catch (BadLocationException ex) {
                    log.error(ex);
                } catch (ExecutionException ex) {
                    setPathInvalid(UncrustifyBundle.message("uncrustify.settings.executableStatus.notExecutable"));
                }
            }
        }
    }
}
