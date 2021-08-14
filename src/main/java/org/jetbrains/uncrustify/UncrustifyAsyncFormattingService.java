package org.jetbrains.uncrustify;

import com.intellij.application.options.CodeStyle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessAdapter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class UncrustifyAsyncFormattingService extends AsyncDocumentFormattingService {

    private static final Logger log = Logger.getInstance(UncrustifyAsyncFormattingService.class);
    private static final List<String> supportedLanguagesIds = List.of(
            "C", "CPP", "D", "CS", "JAVA", "PAWN", "OC", "OC+", "VALA");

    @Override
    protected @Nullable FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest formattingRequest) {
        return new UncrustifyFormattingTask(formattingRequest);
    }

    @Override
    protected @NotNull String getNotificationGroupId() {
        return "Uncrustify Plugin";
    }

    @Override
    protected @NotNull @NlsSafe String getName() {
        return "Uncrustify";
    }

    @Override
    public @NotNull Set<Feature> getFeatures() {
        return Set.of();
    }

    @Override
    public boolean canFormat(@NotNull PsiFile file) {
        UncrustifySettingsState settings = UncrustifySettingsState.getInstance(file.getProject());
        String langId = file.getLanguage().getID();

        return settings.formattingEnabled && supportedLanguagesIds.stream().anyMatch(langId::equalsIgnoreCase);
    }

    protected static class UncrustifyFormattingTask implements FormattingTask {
        private final AsyncFormattingRequest formattingRequest;
        private OSProcessHandler uncrustifyHandler;

        public UncrustifyFormattingTask(AsyncFormattingRequest formattingRequest) {
            this.formattingRequest = formattingRequest;
        }

        @Override
        public boolean cancel() {
            if (uncrustifyHandler == null || !uncrustifyHandler.getProcess().isAlive()) {
                return false;
            }
            uncrustifyHandler.destroyProcess();
            return true;
        }

        @Override
        public void run() {
            log.info("Running Uncrustify");

            UncrustifySettingsState settings = UncrustifySettingsState.getInstance(formattingRequest.getContext().getProject());

            String text = formattingRequest.getDocumentText();
            try {
                Path configPath = FileUtil.createTempFile("ijuncrustify", ".cfg", true).toPath();

                UncrustifyCodeStyleExporter.export(
                        configPath,
                        CodeStyle.getLanguageSettings(formattingRequest.getContext().getContainingFile()));

                GeneralCommandLine commandLine = new GeneralCommandLine()
                        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                        .withExePath(settings.executablePath)
                        .withParameters(
                                "-c",
                                configPath.toString(),
                                "-l",
                                formattingRequest.getContext().getContainingFile().getLanguage().getID());

                uncrustifyHandler = new OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8));
                uncrustifyHandler.addProcessListener(new CapturingProcessAdapter() {
                    @Override
                    public void processTerminated(@NotNull ProcessEvent event) {
                        super.processTerminated(event);

                        int exitCode = getOutput().getExitCode();
                        if (exitCode != 0) {
                            log.warn(String.format("uncrustify exitCode: %d", exitCode));
                            log.debug(getOutput().getStderr());
                            //TODO try to extract uncrustify error message
                            formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                                    String.format(UncrustifyBundle.message("uncrustify.process.error.exitCode"), exitCode));
                        } else {
                            formattingRequest.onTextReady(getOutput().getStdout());
                        }

                    }
                });
                uncrustifyHandler.startNotify();

                try(OutputStreamWriter osw = new OutputStreamWriter(uncrustifyHandler.getProcessInput())) {
                    osw.write(text);
                } catch(IOException e) {
                    log.warn("uncrustify service failed: " + e.getMessage());
                    log.debug(e);
                    formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                            UncrustifyBundle.message("uncrustify.process.error.generalException"));
                }
            } catch(IOException | ExecutionException e) {
                log.warn("uncrustify service failed: " + e.getMessage());
                log.debug(e);
                formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                        UncrustifyBundle.message("uncrustify.process.error.generalException"));
            }
        }
    }
}
