package org.jetbrains.uncrustify;

import com.intellij.application.options.CodeStyle;
import com.intellij.formatting.service.FormattingService;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.uncrustify.settings.UncrustifyFormatSettings;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;
import org.jetbrains.uncrustify.util.UncrustifyConfigFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class UncrustifyServiceTest extends BaseUncrustifyTest {
    protected FormattingService myUncrustifyService;

    @BeforeEach
    private void setUpService() {
        List<FormattingService> serviceList = FormattingService.EP_NAME.getExtensionList();
        FormattingService uncrustifyService = serviceList.stream()
                .filter((s) -> s instanceof UncrustifyAsyncFormattingService)
                .findFirst()
                .orElse(null);
        if (uncrustifyService == null) {
            Assertions.fail("Could not find UncrustifyAsyncFormattingService among FormattingService extension points");
        }
        myUncrustifyService = uncrustifyService;
    }

    @Test
    public void testCanFormatValidExtension() {
        myFixture.configureByFile("helloworld.java");

        CodeStyle.getCustomSettings(myFixture.getFile(), UncrustifyFormatSettings.class).ENABLED = false;
        Assertions.assertFalse(
                myUncrustifyService.canFormat(myFixture.getFile()),
                "canFormat should answer false when Uncrustify formatting is disabled");
        CodeStyle.getCustomSettings(myFixture.getFile(), UncrustifyFormatSettings.class).ENABLED = true;
        Assertions.assertTrue(
                myUncrustifyService.canFormat(myFixture.getFile()),
                "canFormat should answer true when Uncrustify formatting is enabled and file extension is acceptable");
    }

    @Test
    public void testCanFormatInvalidExtension() {
        myFixture.configureByFile("helloworld.rs");
        CodeStyle.getCustomSettings(myFixture.getFile(), UncrustifyFormatSettings.class).ENABLED = false;
        Assertions.assertFalse(
                myUncrustifyService.canFormat(myFixture.getFile()),
                "canFormat should answer false when Uncrustify formatting is disabled");
        CodeStyle.getCustomSettings(myFixture.getFile(), UncrustifyFormatSettings.class).ENABLED = true;
        Assertions.assertFalse(myUncrustifyService.canFormat(
                        myFixture.getFile()),
                "canFormat should answer false when file extension is not acceptable, even when Uncrustify formatting is enabled");
    }

    @Test
    public void testConfigInProjectSelected() {
        VirtualFile projectConfig = myFixture.copyFileToProject("valid.cfg", UncrustifyConfigFile.PROJECT_CONFIG_PATH);
        UncrustifySettingsState.getInstance().configPath = "";
        Assertions.assertEquals(projectConfig.getPath(), UncrustifyConfigFile.getConfigPath(myFixture.getProject()));
    }

    @Test
    public void testConfigInProjectIsSelectedOverCustom() {
        VirtualFile projectConfig = myFixture.copyFileToProject("valid.cfg", UncrustifyConfigFile.PROJECT_CONFIG_PATH);
        UncrustifySettingsState.getInstance().configPath = Path.of(myFixture.getTestDataPath(), "valid.cfg").toString();
        Assertions.assertEquals(projectConfig.getPath(), UncrustifyConfigFile.getConfigPath(myFixture.getProject()));
    }

    @Test
    public void testCustomConfigIsSelected() {
        UncrustifySettingsState.getInstance().configPath = Path.of(myFixture.getTestDataPath(), "valid.cfg").toString();
        Assertions.assertEquals(UncrustifySettingsState.getInstance().configPath, UncrustifyConfigFile.getConfigPath(myFixture.getProject()));
    }

    @Test
    public void testNoConfigToBeSelected() {
        UncrustifySettingsState.getInstance().configPath = null;
        Assertions.assertNull(UncrustifyConfigFile.getConfigPath(myFixture.getProject()));
    }
}
