package org.jetbrains.uncrustify.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "UncrustifySettings",
        storages = {@Storage(value = "uncrustifyPluginSettings.xml", roamingType = RoamingType.DISABLED)}
)
public class UncrustifySettingsState implements PersistentStateComponent<UncrustifySettingsState> {
    public String executablePath = "";
    public String configPath = "";

    public static UncrustifySettingsState getInstance() {
        return ApplicationManager.getApplication().getService(UncrustifySettingsState.class);
    }

    @Nullable
    @Override
    public UncrustifySettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull UncrustifySettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
