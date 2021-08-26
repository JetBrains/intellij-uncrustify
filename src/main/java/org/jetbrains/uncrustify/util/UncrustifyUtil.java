package org.jetbrains.uncrustify.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class UncrustifyUtil {
    private static final Logger log = Logger.getInstance(UncrustifyUtil.class);

    public static final List<String> supportedLanguagesIds = List.of(
            "C", "CPP", "D", "CS", "JAVA", "PAWN", "OC", "OC+", "VALA");

    public static @NotNull OSProcessHandler createProcessHandler(@NotNull String path, String... params) throws ExecutionException {
        GeneralCommandLine commandLine = new GeneralCommandLine()
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withExePath(path)
                .withParameters(params);

        return new OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8));
    }

    public static void setProcessHandlerTimeout(@NotNull OSProcessHandler processHandler, long milis) {
        processHandler.executeTask(() -> {
            try {
                Thread.sleep(milis);
                processHandler.destroyProcess();
            } catch (InterruptedException ignored) {
            }
        });
    }

    @Contract(pure = true)
    public static int max(int @NotNull ... ints) {
        if (ints.length == 0) {
            throw new IllegalArgumentException();
        }

        int candidate = ints[0];
        for (int i = 1; i < ints.length; ++i) {
            if (ints[i] > candidate) {
                candidate = ints[i];
            }
        }
        return candidate;
    }

    @Contract(pure = true)
    public static int min(int @NotNull ... ints) {
        if (ints.length == 0) {
            throw new IllegalArgumentException();
        }

        int candidate = ints[0];
        for (int i = 1; i < ints.length; ++i) {
            if (ints[i] < candidate) {
                candidate = ints[i];
            }
        }
        return candidate;
    }
}
