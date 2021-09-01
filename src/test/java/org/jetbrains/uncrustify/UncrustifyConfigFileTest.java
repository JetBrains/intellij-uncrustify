package org.jetbrains.uncrustify;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;
import org.jetbrains.uncrustify.util.UncrustifyConfigFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class UncrustifyConfigFileTest extends BaseUncrustifyTest {
    @Test
    public void testVerifyValid() throws ExecutionException {
        UncrustifyConfigFile.verify(
                myExecutablePath,
                Path.of(getTestDataPath(), "valid.cfg").toString(),
                new UncrustifyConfigFile.VerificationListener() {
                    @Override
                    public void onValid() {

                    }

                    @Override
                    public void onInvalid(String output) {
                        Assertions.fail();
                    }
                },
                true);
    }

    @Test
    public void testVerifyInvalid() throws ExecutionException {
        UncrustifyConfigFile.verify(
                myExecutablePath,
                Path.of(getTestDataPath(), "invalid.cfg").toString(),
                new UncrustifyConfigFile.VerificationListener() {
                    @Override
                    public void onValid() {
                        Assertions.fail();
                    }

                    @Override
                    public void onInvalid(String output) {

                    }
                },
                true);
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
        Assertions.assertEquals(projectConfig.getCanonicalPath(), UncrustifyConfigFile.getConfigPath(myFixture.getProject()));
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
