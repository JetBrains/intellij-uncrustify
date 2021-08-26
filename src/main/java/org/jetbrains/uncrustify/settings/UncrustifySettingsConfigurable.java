package org.jetbrains.uncrustify.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;

import javax.swing.*;

@SuppressWarnings("UnstableApiUsage")
public class UncrustifySettingsConfigurable implements Configurable {

    private UncrustifySettingsComponent mySettingsComponent;

    public UncrustifySettingsConfigurable() {}

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return UncrustifyBundle.message("uncrustify.settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        mySettingsComponent = new UncrustifySettingsComponent(null);
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance();
        boolean modified = !mySettingsComponent.getConfigPath().equals(settings.executablePath);
        modified |= !mySettingsComponent.getExecutablePath().equals(settings.executablePath);
        return modified;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public void apply() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance();

        settings.executablePath = mySettingsComponent.getExecutablePath();
        settings.configPath = mySettingsComponent.getConfigPath();
    }

    @Override
    public void reset() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance();
        mySettingsComponent.setExecutablePath(settings.executablePath);
        mySettingsComponent.setConfigPath(settings.configPath);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
