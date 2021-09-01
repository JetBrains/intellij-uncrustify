package org.jetbrains.uncrustify.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessAdapter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyConfigWriter;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

public class UncrustifyConfigFile {
    private static final Logger log = Logger.getInstance(UncrustifyConfigFile.class);

    private static final String JAVA_SNIPPET = "public class Uncrustify {public static void main(String args[]){System.out.println(\"Hello Uncrustify!\");}}";

    public interface VerificationListener {
        void onValid();

        void onInvalid(String output);
    }

    public static final String PROJECT_CONFIG_PATH = "uncrustify.cfg";

    public enum Location {

    }

    /**
     * There are 3 options for the location of the config file (sorted desc by priority):
     * <ol>
     *     <li>a file named 'uncrustify.cfg' in the project's directory</li>
     *     <li>a file at custom path specified in Tools | Uncrustify settings</li>
     *     <li>a temporary file automatically generated from IntelliJ code style settings (for this case, this function returns {@code null})</li>
     * </ol>
     */
    public static @Nullable String getConfigPath(@NotNull Project project) {
        String path = getProjectConfigPath(project);
        if (path != null) {
            return path;
        }

        path = getSettingConfigPath();
        return path;
    }

    public static @Nullable String getProjectConfigPath(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            VirtualFile projectConfig = projectDir.findChild(PROJECT_CONFIG_PATH);
            if (projectConfig != null) {
                return projectConfig.getPath();
            }
        }
        return null;
    }

    public static @Nullable String getSettingConfigPath() {
        String configPath = UncrustifySettingsState.getInstance().configPath;
        if (configPath != null && !configPath.isBlank()) {
            return configPath;
        } else {
            return null;
        }
    }

    public static void verify(@NotNull String executablePath, @NotNull String configPath, @NotNull VerificationListener listener, boolean block) throws ExecutionException {
        UncrustifyExecutable.executeWithProcessListener(
                executablePath,
                List.of("-c", configPath, "-l", "JAVA"),
                JAVA_SNIPPET,
                new CapturingProcessAdapter() {
                    public void processTerminated(@NotNull ProcessEvent event) {
                        super.processTerminated(event);

                        int exitCode = event.getExitCode();
                        if (exitCode == 0) {
                            listener.onValid();
                        } else {
                            listener.onInvalid(getOutput().getStderr());
                        }
                    }
                },
                block);
    }

    public static CommonCodeStyleSettings findRelevantCommonCodeStyleSettings(@NotNull CodeStyleSettings settings) {
        return UncrustifyUtil.supportedLanguagesIds.stream()
                .map(Language::findLanguageByID)
                .filter(Objects::nonNull)
                .map(settings::getCommonSettings)
                .findFirst()
                .orElseGet(() -> settings.getCommonSettings(Language.ANY));
    }

    public static void exportCodeStyle(
            @NotNull Path output,
            @NotNull CommonCodeStyleSettings settings) throws IOException {
        try (UncrustifyConfigWriter writer = new UncrustifyConfigWriter(Files.newBufferedWriter(
                output,
                Charset.defaultCharset(),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE))
        ) {
            exportCodeStyle(writer, settings);
        }
    }

    public static void exportCodeStyle(@NotNull UncrustifyConfigWriter writer, @NotNull CommonCodeStyleSettings settings) throws IOException {
        writer.write_comment("Generated by org.jetbrains.uncrustify plugin from IntelliJ code style settings");
        writer.write_option("newlines", "lf");
        exportCommon(writer, settings);
    }

    public static void exportCodeStyle(@NotNull OutputStream outputStream, @NotNull CommonCodeStyleSettings settings) throws IOException {
        try (UncrustifyConfigWriter writer = new UncrustifyConfigWriter(new BufferedWriter(new OutputStreamWriter(outputStream)))) {
            exportCodeStyle(writer, settings);
        }
    }

    @Contract(pure = true)
    private static @NotNull String sp_opt(boolean active) {
        /* From https://github.com/uncrustify/uncrustify/blob/master/src/space.cpp:
             "Ignore" means do not change it.
             "Add" in the context of spaces means make sure there is at least 1.
             "Add" elsewhere means make sure one is present.
             "Remove" mean remove the space/brace/newline/etc.
             "Force" in the context of spaces means ensure that there is exactly 1.
             "Force" in other contexts means the same as "add".
        */
        return active ? "force" : "remove";
    }

    private static void exportCommon(@NotNull UncrustifyConfigWriter writer, @NotNull CommonCodeStyleSettings settings) throws IOException {

        //TODO input_tab_size and output_tab_size, how do they work?

        // start Indenting
        // There are a lot more Indenting Options in Uncrustify, but this is all there can be done with base common indent options
        CommonCodeStyleSettings.IndentOptions opts = settings.getIndentOptions();
        assert opts != null : "No indenting options";

        writer.write_option("indent_class", String.valueOf(!settings.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS));
        writer.write_option("indent_columns", String.valueOf(opts.INDENT_SIZE));
        writer.write_option("indent_continue", String.valueOf(opts.CONTINUATION_INDENT_SIZE));
        //TODO Smart Tabs -- test this. It probably is not 100% the same behaviour.
        {
            int indent_with_tabs = 0;
            if (opts.USE_TAB_CHARACTER) {
                indent_with_tabs = 1;
                if (opts.SMART_TABS) {
                    indent_with_tabs = 2;
                }
            }
            writer.write_option("indent_param", String.valueOf(indent_with_tabs));
        }
        writer.write_option("indent_single_newlines", String.valueOf(opts.KEEP_INDENTS_ON_EMPTY_LINES));
        if (opts.LABEL_INDENT_ABSOLUTE) {
            writer.write_option("indent_label", String.valueOf(opts.LABEL_INDENT_SIZE - 1));
        } else {
            if (opts.LABEL_INDENT_SIZE > 0) {
                log.warn(String.format(
                        "Uncrustify does not support relative label indents >0, truncating %d to 0",
                        opts.LABEL_INDENT_SIZE));
            }
            writer.write_option("indent_label", String.valueOf(0));
        }
        // end Indenting

        // start Spacing
        // Arithmetic operator options aren't as granular in Uncrustify as they are in IntelliJ
        writer.write_option("sp_arith_additive", sp_opt(settings.SPACE_AROUND_ADDITIVE_OPERATORS));
        //   using multiplicative operators as the value, but sp_arith also controls shifts and bitwise operators
        writer.write_option("sp_arith", sp_opt(settings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS));

        writer.write_option("sp_assign", sp_opt(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS));
        writer.write_option("sp_bool", sp_opt(settings.SPACE_AROUND_LOGICAL_OPERATORS));
        writer.write_option("sp_compare", sp_opt(settings.SPACE_AROUND_RELATIONAL_OPERATORS));
        // SPACE_AROUND_UNARY_OPERATOR controls ~, !, -, +, --, ++, (* (deref) and & (address-of)?)
        // sp_sign controls unary -, +
        writer.write_option("sp_sign", sp_opt(settings.SPACE_AROUND_UNARY_OPERATOR));
        // sp_inv controls ~
        writer.write_option("sp_inv", sp_opt(settings.SPACE_AROUND_UNARY_OPERATOR));
        // sp_addr controls & (excluding type definitions, like const string& in C++)
        writer.write_option("sp_addr", sp_opt(settings.SPACE_AROUND_UNARY_OPERATOR));
        // sp_incdec controls --, ++ (post- and pre-)
        writer.write_option("sp_incdec", sp_opt(settings.SPACE_AROUND_UNARY_OPERATOR));
        // sp_not controls !
        writer.write_option("sp_not", sp_opt(settings.SPACE_AROUND_UNARY_OPERATOR));
        // sp_deref contorls unary *
        writer.write_option("sp_deref", sp_opt(settings.SPACE_AROUND_UNARY_OPERATOR));

        writer.write_option("sp_before_semi_for", sp_opt(settings.SPACE_BEFORE_SEMICOLON));
        writer.write_option("sp_before_semi_for_empty", sp_opt(settings.SPACE_BEFORE_SEMICOLON));
        writer.write_option("sp_after_semi_for", sp_opt(settings.SPACE_AFTER_SEMICOLON));
        writer.write_option("sp_after_semi_for_empty", sp_opt(settings.SPACE_AFTER_SEMICOLON));

        // sp_inside_sparen also affects for, switch, while, etc.
        writer.write_option("sp_inside_sparen", sp_opt(settings.SPACE_WITHIN_IF_PARENTHESES));

        // sp_before_sparen also affects for, switch, while, etc.
        writer.write_option("sp_before_sparen", sp_opt(settings.SPACE_BEFORE_IF_PARENTHESES));
        // although for 'while' specifically, it is overrideable
        writer.write_option("sp_while_paren_open", sp_opt(settings.SPACE_BEFORE_WHILE_PARENTHESES));

        writer.write_option("sp_inside_paren_cast", sp_opt(settings.SPACE_WITHIN_CAST_PARENTHESES));
        writer.write_option("sp_after_cast", sp_opt(settings.SPACE_AFTER_TYPE_CAST));

        writer.write_option("sp_func_proto_paren", sp_opt(settings.SPACE_BEFORE_METHOD_PARENTHESES));
        writer.write_option("sp_func_proto_paren_empty", sp_opt(settings.SPACE_BEFORE_METHOD_PARENTHESES));
        writer.write_option("sp_func_def_paren", sp_opt(settings.SPACE_BEFORE_METHOD_PARENTHESES));
        writer.write_option("sp_func_def_paren_empty", sp_opt(settings.SPACE_BEFORE_METHOD_PARENTHESES));

        writer.write_option("sp_fparen_brace", sp_opt(settings.SPACE_BEFORE_METHOD_LBRACE));
        writer.write_option("sp_func_call_paren", sp_opt(settings.SPACE_BEFORE_METHOD_CALL_PARENTHESES));

        writer.write_option("sp_inside_fparen", sp_opt(settings.SPACE_WITHIN_METHOD_PARENTHESES));
        writer.write_option("sp_inside_fparens", sp_opt(settings.SPACE_WITHIN_METHOD_PARENTHESES));

        writer.write_option("sp_inside_paren", sp_opt(settings.SPACE_WITHIN_PARENTHESES));
        writer.write_option("sp_paren_paren", sp_opt(settings.SPACE_WITHIN_PARENTHESES));

        writer.write_option("sp_inside_square", sp_opt(settings.SPACE_WITHIN_BRACKETS));
        writer.write_option("sp_inside_square_empty", sp_opt(false));

        // sp_sparen_brace controls all control statements (if, for, switch, while, etc.)
        writer.write_option("sp_sparen_brace", sp_opt(settings.SPACE_BEFORE_IF_LBRACE));
        writer.write_option("sp_do_brace_open", sp_opt(settings.SPACE_BEFORE_DO_LBRACE));
        writer.write_option("sp_try_brace", sp_opt(settings.SPACE_BEFORE_TRY_LBRACE));

        writer.write_option("sp_brace_catch", sp_opt(settings.SPACE_BEFORE_CATCH_KEYWORD));
        writer.write_option("sp_brace_finally", sp_opt(settings.SPACE_BEFORE_FINALLY_KEYWORD));
        writer.write_option("sp_finally_brace", sp_opt(settings.SPACE_BEFORE_FINALLY_LBRACE));
        writer.write_option("sp_brace_else", sp_opt(settings.SPACE_BEFORE_ELSE_KEYWORD));
        writer.write_option("sp_else_brace", sp_opt(settings.SPACE_BEFORE_ELSE_LBRACE));

        // Ternary Operator spacings
        writer.write_option("sp_cond_colon_before", sp_opt(settings.SPACE_BEFORE_COLON));
        writer.write_option("sp_cond_colon_after", sp_opt(settings.SPACE_AFTER_COLON));
        writer.write_option("sp_cond_question_before", sp_opt(settings.SPACE_BEFORE_QUEST));
        writer.write_option("sp_cond_question_after", sp_opt(settings.SPACE_AFTER_QUEST));

        writer.write_option("nl_start_of_file", String.valueOf(settings.BLANK_LINES_BEFORE_PACKAGE));

        writer.write_option("nl_max", String.valueOf(UncrustifyUtil.max(
                settings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                settings.KEEP_BLANK_LINES_IN_CODE,
                settings.KEEP_BLANK_LINES_BETWEEN_PACKAGE_DECLARATION_AND_HEADER)+1));

        writer.write_option("nl_max_blank_in_func", String.valueOf(settings.KEEP_BLANK_LINES_IN_CODE+1));
        writer.write_option("nl_inside_empty_func", String.valueOf(settings.KEEP_BLANK_LINES_IN_CODE+1));

        writer.write_option("nl_before_func_body_def", String.valueOf(settings.BLANK_LINES_BEFORE_METHOD_BODY+1));
        writer.write_option("nl_before_func_class_def", String.valueOf(settings.BLANK_LINES_BEFORE_METHOD_BODY+1));
        writer.write_option("nl_before_func_class_proto", String.valueOf(settings.BLANK_LINES_BEFORE_METHOD_BODY+1));
        writer.write_option("nl_before_func_body_proto", String.valueOf(settings.BLANK_LINES_BEFORE_METHOD_BODY+1));
//        writer.write_option("nl_after_func_proto_group", String.valueOf(settings.BLANK_LINES_BEFORE_METHOD_BODY)+1);
//        writer.write_option("nl_after_func_class_proto_group", String.valueOf(settings.BLANK_LINES_BEFORE_METHOD_BODY)+1);

        writer.write_option("nl_before_class", String.valueOf(settings.BLANK_LINES_AROUND_CLASS+1));
        writer.write_option("nl_after_class", String.valueOf(settings.BLANK_LINES_AROUND_CLASS+1));

        // Any newlines that aren't explicitly defined will be removed. This is potentially dangerous and can lead to invalid code.
        //  => use the default(=0)
        // writer.write_option("nl_remove_extra_newlines", String.valueOf(1));
    }
}
