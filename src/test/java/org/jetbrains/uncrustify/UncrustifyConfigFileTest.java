package org.jetbrains.uncrustify;

import com.intellij.execution.ExecutionException;
import org.jetbrains.uncrustify.util.UncrustifyConfigFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class UncrustifyConfigFileTest extends BaseUncrustifyTest {
    @Test
    public void testVerifyValid() throws ExecutionException {
        UncrustifyConfigFile.verify(
                executablePath,
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
                executablePath,
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
}
