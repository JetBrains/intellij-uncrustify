package org.jetbrains.uncrustify.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class UncrustifySettingsConfigurable implements Configurable {

    private UncrustifySettingsComponent mySettingsComponent;
    private final Project myProject;

    public UncrustifySettingsConfigurable(@NotNull Project project) {
        myProject = project;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Uncrustify";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mySettingsComponent = new UncrustifySettingsComponent(myProject);
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance(myProject);
        assert mySettingsComponent != null;
        assert settings != null;
        boolean modified = !mySettingsComponent.getUncrustifyExecutablePath().equals(settings.uncrustifyExecutablePath);
        modified |= mySettingsComponent.getUncrustifyFormattingEnabled() != settings.uncrustifyFormattingEnabled;
        return modified;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public void apply() throws ConfigurationException {
        //TODO validate uncrustify executable, throw configuration exception in case of error
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance(myProject);

        settings.uncrustifyExecutablePath = mySettingsComponent.getUncrustifyExecutablePath();
        settings.uncrustifyFormattingEnabled = mySettingsComponent.getUncrustifyFormattingEnabled();
    }

    @Override
    public void reset() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance(myProject);
        mySettingsComponent.setUncrustifyExecutablePath(settings.uncrustifyExecutablePath);
        mySettingsComponent.setUncrustifyFormattingEnabled(settings.uncrustifyFormattingEnabled);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
