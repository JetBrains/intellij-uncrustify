package org.jetbrains.uncrustify.settings;

import com.intellij.application.options.GeneralCodeStyleOptionsProvider;
import com.intellij.ide.DataManager;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;

import javax.swing.*;

public class UncrustifyFormatConfigurable extends CodeStyleSettingsProvider implements GeneralCodeStyleOptionsProvider {
    private SettingsComponent mySettingsComponent;

    @Override
    public void reset(@NotNull CodeStyleSettings settings) {
        UncrustifyFormatSettings uncrustifySettings = settings.getCustomSettings(UncrustifyFormatSettings.class);

        mySettingsComponent.setUncrustifyFormattingEnabled(uncrustifySettings.ENABLED);
    }

    @Override
    public void apply(@NotNull CodeStyleSettings settings) {
        UncrustifyFormatSettings uncrustifySettings = settings.getCustomSettings(UncrustifyFormatSettings.class);

        uncrustifySettings.ENABLED = mySettingsComponent.getUncrustifyFormattingEnabled();
    }

    @Override
    public @Nullable JComponent createComponent() {
        mySettingsComponent = new SettingsComponent();
        mySettingsComponent.getPanel().setBorder(IdeBorderFactory.createTitledBorder(UncrustifyBundle.message("uncrustify.settings.displayName"), false));
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified(@NotNull CodeStyleSettings settings) {
        UncrustifyFormatSettings uncrustifySettings = settings.getCustomSettings(UncrustifyFormatSettings.class);
        return mySettingsComponent.getUncrustifyFormattingEnabled() != uncrustifySettings.ENABLED;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public void apply() {
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

    @Override
    public boolean hasSettingsPage() {
        return false;
    }

    @Override
    public @Nullable CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new UncrustifyFormatSettings(settings);
    }

    private static class SettingsComponent {
        private final JPanel myMainPanel;
        private final JBCheckBox myFormattingEnabled = new JBCheckBox(UncrustifyBundle.message("uncrustify.settings.enableFormatting.label"));
        private final ActionLink myLinkToUncrustifySettings = new ActionLink(
                UncrustifyBundle.message("uncrustify.settings.enableFormatting.configureExecutableLink"),
                (e) -> {
                    Settings settings = DataManager.getInstance().getDataContext((ActionLink)e.getSource()).getData(Settings.KEY);
                    if (settings != null) {
                        settings.select(settings.find("uncrustify"));
                    }
                });

        private void updateLink() {
            if (myFormattingEnabled.isSelected()) {
                UncrustifySettingsState settings = UncrustifySettingsState.getInstance();
                myLinkToUncrustifySettings.setVisible(settings.executablePath == null || settings.executablePath.isBlank());
            } else {
                myLinkToUncrustifySettings.setVisible(false);
            }
        }

        public SettingsComponent() {
            myMainPanel = FormBuilder.createFormBuilder()
                    .addComponent(myFormattingEnabled, 1)
                    .addComponent(myLinkToUncrustifySettings)
                    .addComponentFillVertically(new JPanel(), 0)
                    .getPanel();

            myFormattingEnabled.addActionListener(e -> updateLink());
        }

        public JPanel getPanel() {
            return myMainPanel;
        }

        public JComponent getPreferredFocusedComponent() {
            return myFormattingEnabled;
        }

        public boolean getUncrustifyFormattingEnabled() {
            return myFormattingEnabled.isSelected();
        }

        public void setUncrustifyFormattingEnabled(boolean newStatus) {
            myFormattingEnabled.setSelected(newStatus);
            updateLink();
        }
    }
}
