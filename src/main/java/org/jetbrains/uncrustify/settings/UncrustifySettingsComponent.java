package org.jetbrains.uncrustify.settings;

import com.intellij.execution.ExecutionException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;
import org.jetbrains.uncrustify.ui.DocumentVerifierComponent;
import org.jetbrains.uncrustify.util.UncrustifyConfigFile;
import org.jetbrains.uncrustify.util.UncrustifyExecutable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

public class UncrustifySettingsComponent {
    private static final Logger log = Logger.getInstance(UncrustifySettingsComponent.class);

    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myExecutablePath = new TextFieldWithBrowseButton();
    private final VersionVerifierComponent myVersionCheckField = new VersionVerifierComponent(myExecutablePath.getTextField().getDocument());
    private final TextFieldWithBrowseButton myConfigPath = new TextFieldWithBrowseButton();
    private final ConfigVerifierComponent myConfigCheckField = new ConfigVerifierComponent(
            myConfigPath.getTextField().getDocument(), myExecutablePath.getTextField().getDocument());

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
        myMainPanel.add(myVersionCheckField, bag.nextLine().next().next().insets(0, 5, -1, -1).fillCell());
        myMainPanel.add(new JBLabel(UncrustifyBundle.message("uncrustify.settings.configPath.label")), bag.nextLine().next());
        myMainPanel.add(myConfigPath, bag.next().fillCell());
        myMainPanel.add(myConfigCheckField, bag.nextLine().next().next().insets(0, 5, -1, -1).fillCell());
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

        myVersionCheckField.setFontSize(UIUtil.FontSize.SMALL);
        myConfigCheckField.setFontSize(UIUtil.FontSize.SMALL);
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
        return myVersionCheckField.isDocumentValid();
    }

    public @NotNull String getConfigPath() {
        return myConfigPath.getText();
    }

    public void setConfigPath(@NotNull String text) {
        myConfigPath.setText(text);
    }

    private static class VersionVerifierComponent extends DocumentVerifierComponent {

        public VersionVerifierComponent(@NotNull Document document) {
            super(document);
        }

        public void setPathIsEmpty() {
            setValid(false);
            setIcon(AllIcons.General.Warning);
            setText(UncrustifyBundle.message("uncrustify.settings.executableStatus.none"));
        }

        public void setPathIsValid(String message) {
            setValid(true);
            setIcon(AllIcons.General.InspectionsOK);
            setText(message);
        }

        public void setPathIsInvalid(String message) {
            setValid(false);
            setIcon(AllIcons.General.Error);
            setText(message);
        }

        public void verifyDocument() {
            try {
                if (getDocument().getLength() <= 0) {
                    setPathIsEmpty();
                } else {
                    setDocumentIsBeingChecked();
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

    private class ConfigVerifierComponent extends DocumentVerifierComponent {
        private @Nullable Runnable hyperlinkListener = null;

        public ConfigVerifierComponent(@NotNull Document document, @NotNull Document executablePath) {
            super(document);

            this.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (hyperlinkListener != null) {
                        hyperlinkListener.run();
                    }
                }
            });

            executablePath.addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    if (isExecutablePathValid()) {
                        verifyDocument();
                    }
                }
            });
        }

        public void setPathIsEmpty() {
            setValid(false);
            setIcon(null);
            setText("");
        }

        public void setPathIsValid() {
            setValid(true);
            setIcon(AllIcons.General.InspectionsOK);
            setText(UncrustifyBundle.message("uncrustify.settings.configStatus.ok"));
        }

        @SuppressWarnings("UnstableApiUsage")
        public void setPathIsInvalidWithLink(String message) {
            setValid(false);
            setIcon(AllIcons.General.Error);
            setTextWithHyperlink(message);
        }

        public void setPathIsInvalid(String message) {
            setValid(false);
            setIcon(AllIcons.General.Error);
            setText(message);
        }

        public void setCouldNotVerify(String message) {
            setValid(false);
            setIcon(AllIcons.General.Warning);
            setText(message);
        }

        @Override
        public void verifyDocument() {
            try {
                if (getDocument().getLength() <= 0) {
                    setPathIsEmpty();
                } else {
                    setDocumentIsBeingChecked();
                    String documentText = getDocument().getText(0, getDocument().getLength());

                    if (!FileUtil.exists(documentText)) {
                        setPathIsInvalid(UncrustifyBundle.message("uncrustify.settings.fileDoesNotExist"));
                        return;
                    }

                    if (!isExecutablePathValid()) {
                        setCouldNotVerify(UncrustifyBundle.message("uncrustify.settings.configStatus.couldNotVerify"));
                        return;
                    }
                    String executablePath = getExecutablePath();

                    UncrustifyConfigFile.verify(
                            executablePath,
                            documentText,
                            new UncrustifyConfigFile.VerificationListener() {
                                @Override
                                public void onInvalid(String output) {
                                    setPathIsInvalidWithLink(UncrustifyBundle.message("uncrustify.settings.configStatus.fail"));

                                    hyperlinkListener = () -> {
                                        DialogBuilder dialogBuilder = new DialogBuilder(myConfigCheckField);
                                        JTextArea textArea = new JTextArea(output, 20, 80);
                                        dialogBuilder.setCenterPanel(ScrollPaneFactory.createScrollPane(textArea));
                                        dialogBuilder.setPreferredFocusComponent(textArea);
                                        dialogBuilder.setTitle(UncrustifyBundle.message("uncrustify.process.output.title"));
                                        dialogBuilder.addCloseButton();

                                        dialogBuilder.show();
                                    };
                                }

                                @Override
                                public void onValid() {
                                    setPathIsValid();
                                }
                            },
                            false);
                }
            } catch (BadLocationException ex) {
                log.error(ex);
            } catch (ExecutionException ex) {
                setCouldNotVerify(UncrustifyBundle.message("uncrustify.settings.configStatus.couldNotVerify"));
            }
        }
    }
}
