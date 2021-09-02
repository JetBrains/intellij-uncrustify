package org.jetbrains.uncrustify;

import com.intellij.application.options.CodeStyle;
import com.intellij.formatting.FormatTextRanges;
import com.intellij.formatting.service.FormattingService;
import com.intellij.openapi.command.WriteCommandAction;
import org.jetbrains.uncrustify.settings.UncrustifyFormatSettings;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;
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
    public void testFormat() throws InterruptedException {
        myFixture.configureByFile("helloworld.java");
        CodeStyle.getCustomSettings(myFixture.getFile(), UncrustifyFormatSettings.class).ENABLED = true;
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance();
        settings.executablePath = myExecutablePath;
        settings.configPath = Path.of(getTestDataPath(), "valid.cfg").toAbsolutePath().toString();
        String originalText = myFixture.getFile().getText();
        WriteCommandAction.writeCommandAction(myFixture.getProject()).run(() ->
                myUncrustifyService.formatRanges(
                        myFixture.getFile(),
                        new FormatTextRanges(myFixture.getFile().getTextRange(), true), false, false));
        //TODO This line is a workaround to make sure formatted text is ready when the assertion is executed. Platform
        // fix is needed before this line can be removed.
        Thread.sleep(1000);
        String formattedText = myFixture.getFile().getText();
        Assertions.assertNotEquals(originalText, formattedText);
    }
}