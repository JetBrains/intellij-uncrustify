package org.jetbrains.uncrustify;

import com.intellij.application.options.CodeStyle;
import com.intellij.execution.ExecutionException;
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
import org.jetbrains.uncrustify.settings.UncrustifyFormatSettings;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class UncrustifyAsyncFormattingService extends AsyncDocumentFormattingService {

    private static final Logger log = Logger.getInstance(UncrustifyAsyncFormattingService.class);

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
        UncrustifyFormatSettings settings = CodeStyle.getCustomSettings(file, UncrustifyFormatSettings.class);
        String langId = file.getLanguage().getID();

        return settings.ENABLED && UncrustifyUtil.supportedLanguagesIds.stream().anyMatch(langId::equalsIgnoreCase);
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

        private UncrustifySettingsState getSettings() {
            return UncrustifySettingsState.getInstance();
        }

        protected void format(@NotNull String configPath, @NotNull String uncrustifyLanguageId) {
            String text = formattingRequest.getDocumentText();
            UncrustifyFormatSettings settings = CodeStyle.getCustomSettings(formattingRequest.getContext().getContainingFile(), UncrustifyFormatSettings.class);
            try {
                uncrustifyHandler = UncrustifyUtil.createProcessHandler(
                        getSettings().executablePath,
                        "-c", configPath, "-l", uncrustifyLanguageId);

                uncrustifyHandler.addProcessListener(new CapturingProcessAdapter() {
                    @Override
                    public void processTerminated(@NotNull ProcessEvent event) {
                        super.processTerminated(event);

                        int exitCode = getOutput().getExitCode();
                        if (exitCode != 0) {
                            log.warn(String.format("uncrustify exitCode: %d", exitCode));
                            log.debug(getOutput().getStdout());
                            log.debug(getOutput().getStderr());
                            formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                                    String.format(UncrustifyBundle.message("uncrustify.process.error.exitCode"), exitCode));
                        } else {
                            formattingRequest.onTextReady(getOutput().getStdout());
                        }

                    }
                });
                uncrustifyHandler.startNotify();

                try (OutputStreamWriter osw = new OutputStreamWriter(uncrustifyHandler.getProcessInput())) {
                    osw.write(text);
                } catch (IOException e) {
                    log.warn("uncrustify service failed: " + e.getMessage());
                    log.debug(e);
                    formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                            UncrustifyBundle.message("uncrustify.process.error.generalException"));
                }
            } catch (ExecutionException e) {
                log.warn("uncrustify service failed: " + e.getMessage());
                log.debug(e);
                formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                        UncrustifyBundle.message("uncrustify.process.error.generalException"));
            }
        }

        protected @NotNull String prepareConfig() throws IOException {
            UncrustifyFormatSettings settings = CodeStyle.getCustomSettings(formattingRequest.getContext().getContainingFile(), UncrustifyFormatSettings.class);

            String configPath;
            if (getSettings().useCustomConfig) {
                configPath = getSettings().customConfigPath;
            } else {
                Path tempFilePath = FileUtil.createTempFile("ijuncrustify", ".cfg", true).toPath();
                UncrustifyUtil.exportCodeStyle(tempFilePath, CodeStyle.getLanguageSettings(formattingRequest.getContext().getContainingFile()));
                configPath = tempFilePath.toString();
            }

            return configPath;
        }

        protected void format(@NotNull String configPath) {
            PsiFile containingFile = formattingRequest.getContext().getContainingFile();
            format(configPath, containingFile.getLanguage().getID());
        }

        @Override
        public void run() {
            log.info("Running Uncrustify");
            try {
                format(prepareConfig());
            } catch (IOException ex) {
                log.warn("uncrustify service failed: " + ex.getMessage());
                log.debug(ex);
                formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                        UncrustifyBundle.message("uncrustify.process.error.generalException"));
            }
        }
    }
}
