package org.jetbrains.uncrustify;

import org.jetbrains.uncrustify.util.UncrustifyExecutable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UncrustifyExecutableTest extends BaseUncrustifyTest {
    @Test
    public void testVerifyVersion() {
        Assertions.assertNotNull(
                UncrustifyExecutable.verifyVersion("some unrelated text\nUncrustify-1.0.0 some more unrelated text\n"),
                "Valid 'uncrustify --version' output didn't verify");
    }

}