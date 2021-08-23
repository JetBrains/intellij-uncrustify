package org.jetbrains.uncrustify;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SchemeExporter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class UncrustifyCodeStyleExporter extends SchemeExporter<CodeStyleScheme> {
    private static final Logger log = Logger.getInstance(UncrustifyCodeStyleExporter.class);

    @Override
    public String getDefaultFileName(@NotNull String schemeName) {
        return "";
    }

    @Override
    public void exportScheme(@Nullable Project project, @NotNull CodeStyleScheme scheme, @NotNull OutputStream outputStream) throws Exception {
        UncrustifyUtil.exportCodeStyle(outputStream, UncrustifyUtil.findRelevantCommonCodeStyleSettings(scheme.getCodeStyleSettings()));
    }

    @Override
    public String getExtension() {
        return "config";
    }
}
