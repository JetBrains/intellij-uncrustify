package org.jetbrains.uncrustify.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "org.jetbrains.uncrustify.settings.UncrustifySettingsState",
        storages = {@Storage("UncrustifyPluginSettings.xml")}
)
public class UncrustifySettingsState implements PersistentStateComponent<UncrustifySettingsState> {
    public String uncrustifyExecutablePath;
    public boolean uncrustifyFormattingEnabled;

    public static UncrustifySettingsState getInstance(@NotNull Project project) {
        return project.getService(UncrustifySettingsState.class);
    }

    @Nullable
    @Override
    public UncrustifySettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull UncrustifySettingsState state) {
        //TODO perform validation?
        XmlSerializerUtil.copyBean(state, this);
    }
}
