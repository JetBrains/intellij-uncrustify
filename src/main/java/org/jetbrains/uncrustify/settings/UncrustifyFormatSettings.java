package org.jetbrains.uncrustify.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class UncrustifyFormatSettings extends CustomCodeStyleSettings {
    public boolean ENABLED = false;

    public UncrustifyFormatSettings(CodeStyleSettings container) {
        super("UncrustifyFormatSettings", container);
    }
}
