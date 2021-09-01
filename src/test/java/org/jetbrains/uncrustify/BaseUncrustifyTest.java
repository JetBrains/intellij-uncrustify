package org.jetbrains.uncrustify;

import com.intellij.execution.ExecutionException;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.uncrustify.util.UncrustifyExecutable;
import org.junit.jupiter.api.*;

import java.nio.file.Path;

public class BaseUncrustifyTest extends BasePlatformTestCase {
    protected static String myExecutablePath;

    @BeforeAll
    public static void setUpExecutable() {
        myExecutablePath = System.getProperty("uncrustify.executablePath", null);
        if (myExecutablePath == null || myExecutablePath.equals("null")) {
            Assertions.fail("Uncrustify executable was not specified. Related tests cannot run.");
        }

        verifyExecutable();
    }

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public static void verifyExecutable() {
        try {
            UncrustifyExecutable.verify(
                    myExecutablePath,
                    new UncrustifyExecutable.VerificationListener() {
                        @Override
                        public void onInvalid() {
                            Assertions.fail("Invalid Uncrustify executable. (Reason: Could not validate version) (Path: '" + myExecutablePath + "')");
                        }

                        @Override
                        public void onValid(String version) {
                        }
                    },
                    true);
        } catch (ExecutionException e) {
            Assertions.fail("Could not run Uncrustify executable. (Reason: " + e.getMessage() + ") (Path: " + myExecutablePath + ")");
        }
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src", "test", "testData").normalize().toAbsolutePath().toString();
    }
}
