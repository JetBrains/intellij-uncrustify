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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uncrustify.settings.UncrustifyFormatSettings;
import org.jetbrains.uncrustify.settings.UncrustifySettingsState;
import org.jetbrains.uncrustify.util.UncrustifyConfigFile;
import org.jetbrains.uncrustify.util.UncrustifyExecutable;
import org.jetbrains.uncrustify.util.UncrustifyUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
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

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }

        return settings.ENABLED && UncrustifyUtil.isExtensionSupported(file.getVirtualFile().getName());
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

        protected void format(@NotNull String configPath, @NotNull String filename) {
            String text = formattingRequest.getDocumentText();
            try {
                uncrustifyHandler = UncrustifyExecutable.executeWithProcessListener(
                        getSettings().executablePath,
                        List.of("-c", configPath, "--assume", filename),
                        text,
                        new CapturingProcessAdapter() {
                            @Override
                            public void processTerminated(@NotNull ProcessEvent event) {
                                super.processTerminated(event);

                                int exitCode = getOutput().getExitCode();
                                if (exitCode != 0) {
                                    log.warn(String.format("uncrustify exitCode: %d", exitCode));
                                    log.warn(getOutput().getStdout());
                                    log.warn(getOutput().getStderr());
                                    formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                                            String.format(UncrustifyBundle.message("uncrustify.process.error.exitCode"), exitCode));
                                } else {
                                    formattingRequest.onTextReady(getOutput().getStdout());
                                }
                            }
                        },
                        false);
            } catch (ExecutionException e) {
                log.warn("uncrustify service failed: " + e.getMessage());
                log.debug(e);
                formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                        UncrustifyBundle.message("uncrustify.process.error.generalException"));
            }
        }

        protected @NotNull String prepareConfig() throws IOException {
            String path = UncrustifyConfigFile.getConfigPath(formattingRequest.getContext().getProject());
            if (path != null) {
                return path;
            }

            Path tempFilePath = FileUtil.createTempFile("ijuncrustify", ".cfg", true).toPath();
            UncrustifyConfigFile.exportCodeStyle(tempFilePath, CodeStyle.getLanguageSettings(formattingRequest.getContext().getContainingFile()));
            return tempFilePath.toString();
        }

        protected void format(@NotNull String configPath) {
            VirtualFile virtualFile = formattingRequest.getContext().getVirtualFile();
            if (virtualFile == null) {
                log.warn("VirtualFile is null, cannot format. canFormat method should have prevented this.");
                formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                        UncrustifyBundle.message("uncrustify.process.error.generalException"));
                return;
            }
            format(configPath, virtualFile.getName());
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
