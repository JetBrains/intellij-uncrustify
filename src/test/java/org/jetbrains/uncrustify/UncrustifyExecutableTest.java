package org.jetbrains.uncrustify;

import com.intellij.execution.ExecutionException;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.uncrustify.util.UncrustifyExecutable;
import org.junit.jupiter.api.*;

import java.nio.file.Path;

class UncrustifyExecutableTest extends BasePlatformTestCase {
    private static String executablePath;

    @BeforeAll
    public static void setUpExecutable() throws Exception {
        executablePath = System.getProperty("uncrustify.executablePath", null);
        if (executablePath == null || executablePath.equals("null")) {
            throw new Exception("Uncrustify executable was not specified. Related tests cannot run.");
        }
    }

    @Order(0)
    @Test
    public void testVerify() {
        try {
            UncrustifyExecutable.verify(
                    executablePath,
                    new UncrustifyExecutable.VerificationListener() {
                        @Override
                        public void onInvalid() {
                            Assertions.fail("Invalid Uncrustify executable. (Reason: Could not validate version) (Path: " + executablePath + ")");
                        }

                        @Override
                        public void onValid(String version) {
                        }
                    },
                    true);
        } catch (ExecutionException e) {
            Assertions.fail("Could not run Uncrustify executable. (Reason: " + e.getMessage() + ") (Path: " + executablePath + ")");
        }
    }

    @Test
    public void testVerifyVersion() {
        Assertions.assertNotNull(
                UncrustifyExecutable.verifyVersion("some unrelated text\nUncrustify-1.0.0 some more unrelated text\n"), "Valid 'uncrustify --version' output didn't verify");
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src", "test", "testData").normalize().toAbsolutePath().toString();
    }
}