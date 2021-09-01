package org.jetbrains.uncrustify.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.UncrustifyBundle;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UncrustifyExecutable {
    // example of a version string: Uncrustify_d-0.73.0_f
    //                                         ^        ^
    //                        indicates debug version   |
    //                                               fallback?
    private static final Pattern VERSION_PATTERN = Pattern.compile("Uncrustify(_d)?-((\\d+)\\.(\\d+)\\.(\\d+))(_[a-z])?");

    public static @Nullable String verifyVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.find() ? matcher.group() : null;
    }

    public interface VerificationListener {
        void onInvalid();

        void onValid(String version);
    }

    // Throws in case the file could not be executed
    public static void verify(@NotNull String path, @NotNull VerificationListener listener, boolean block) throws ExecutionException {
        executeWithProcessListener(
                path,
                List.of("--version"),
                null,
                new CapturingProcessAdapter() {
                    public void processTerminated(@NotNull ProcessEvent event) {
                        super.processTerminated(event);

                        int exitCode = event.getExitCode();
                        ProcessOutput processOutput = getOutput();
                        if (exitCode == 0) {
                            String version = verifyVersion(processOutput.getStdout());
                            if (version != null) {
                                listener.onValid(version);
                            } else {
                                listener.onInvalid();
                            }
                        } else {
                            listener.onInvalid();
                        }
                    }
                },
                block);
    }

    public static @NotNull OSProcessHandler executeWithProcessListener(
            @NotNull String executablePath,
            @NotNull List<String> params,
            @Nullable String input,
            @NotNull ProcessListener processListener,
            boolean block) throws ExecutionException {
        OSProcessHandler handler = UncrustifyUtil.createProcessHandler(
                executablePath,
                params);
        handler.addProcessListener(processListener);
        //TODO after some timeout, kill the process (for cases when it doesn't terminate on its own for some reason)
        handler.startNotify();

        if (input != null) {
            try (OutputStreamWriter osw = new OutputStreamWriter(handler.getProcessInput())) {
                osw.write(input);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }
        if (block) {
            handler.waitFor();
        }
        return handler;
    }

}
