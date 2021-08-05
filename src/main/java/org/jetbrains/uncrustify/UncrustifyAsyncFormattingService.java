package org.jetbrains.uncrustify;

import com.intellij.application.options.CodeStyle;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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
        //TODO Find out more about group ids
        return "Uncrustify notification groupId";
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

        return settings.uncrustifyFormattingEnabled && supportedLanguagesIds.stream().anyMatch(langId::equalsIgnoreCase);
    }

    protected class UncrustifyFormattingTask implements FormattingTask {
        private final AsyncFormattingRequest formattingRequest;
        private Process uncrustifyProcess;

        public UncrustifyFormattingTask(AsyncFormattingRequest formattingRequest) {
            this.formattingRequest = formattingRequest;
        }

        @Override
        public boolean cancel() {
            if (uncrustifyProcess == null || !uncrustifyProcess.isAlive()) {
                return false;
            }
            uncrustifyProcess.destroyForcibly();
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

                uncrustifyProcess =
                        new ProcessBuilder(settings.uncrustifyExecutablePath,
                                "-c",
                                configPath.toString(),
                                "-l",
                                formattingRequest.getContext().getContainingFile().getLanguage().getID()).start();
                OutputStreamWriter osw = new OutputStreamWriter(uncrustifyProcess.getOutputStream());
                osw.write(text);
                osw.close();
                int exitCode = uncrustifyProcess.waitFor();

                if (exitCode != 0) {
                    log.error(String.format("uncrustify exitCode: %d", exitCode));
                }
                BufferedReader er = new BufferedReader(new InputStreamReader(uncrustifyProcess.getErrorStream()));
                for (String line; (line = er.readLine()) != null;) {
                    log.debug(line);
                }

                if (exitCode != 0) {
                    formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                            String.format(UncrustifyBundle.message("uncrustify.process.error.exitCode"), exitCode));
                }

                BufferedInputStream bis = new BufferedInputStream(uncrustifyProcess.getInputStream());
                ByteArrayOutputStream formattedTextBuf = new ByteArrayOutputStream();
                int result = bis.read();
                while (result != -1) {
                    formattedTextBuf.write((byte) result);
                    result = bis.read();
                }
                formattingRequest.onTextReady(formattedTextBuf.toString());
            } catch(IOException | InterruptedException e) {
                log.error("uncrustify service failed: " + e.getMessage());
                formattingRequest.onError(UncrustifyBundle.message("uncrustify.process.error.title"),
                        UncrustifyBundle.message("uncrustify.process.error.generalException"));
            }
        }
    }
}
