package org.jetbrains.uncrustify.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;

import javax.swing.*;

@SuppressWarnings("UnstableApiUsage")
public class UncrustifySettingsConfigurable implements Configurable {

    private UncrustifySettingsComponent mySettingsComponent;

//    private final Project myProject;
//
//    public UncrustifySettingsConfigurable(@NotNull Project project) {
//        myProject = project;
//    }

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
        assert mySettingsComponent != null;
        assert settings != null;
        return !mySettingsComponent.getUncrustifyExecutablePath().equals(settings.executablePath);
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Override
    public void apply() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance();

        settings.executablePath = mySettingsComponent.getUncrustifyExecutablePath();
    }

    @Override
    public void reset() {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance();
        mySettingsComponent.setUncrustifyExecutablePath(settings.executablePath);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
