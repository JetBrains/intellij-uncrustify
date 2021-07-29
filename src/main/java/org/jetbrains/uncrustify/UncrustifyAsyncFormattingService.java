package org.jetbrains.uncrustify;

import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Set;

public class UncrustifyAsyncFormattingService extends AsyncDocumentFormattingService {
    @Override
    protected @Nullable FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest formattingRequest) {
        return new UncrustifyFormattingTask(formattingRequest);
    }

    @Override
    protected @NotNull String getNotificationGroupId() {
        //TODO do I need to extend an EP? Experiment with actually making notifications
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

        //getlangueage
        return file.getFileType().getName().equalsIgnoreCase("java");
    }

    protected class UncrustifyFormattingTask implements FormattingTask {

        private static final int READ_BUF_SIZE = 4096;

        private final AsyncFormattingRequest formattingRequest;

        public UncrustifyFormattingTask(AsyncFormattingRequest formattingRequest) {
            this.formattingRequest = formattingRequest;
        }

        @Override
        public boolean cancel() {
            //TODO store process in an instance var and kill the subprocess
            return false;
        }

        @Override
        public void run() {
            //TODO some setting, where the path to uncrustify executable can be set
            // for now, it must be found in $PATH

            // verify that indeed, uncrustify cannot work on ranges
            String text = formattingRequest.getDocumentText();
            try {
                Process uncrustifyProcess =
                        new ProcessBuilder("/Users/vojtech.balik/Work/uncrustify/playground/uncrustify",
                                "-c",
                                "/Users/vojtech.balik/Work/uncrustify/playground/test.cfg",
                                "-l",
                                "JAVA").start();
                OutputStreamWriter osw = new OutputStreamWriter(uncrustifyProcess.getOutputStream());
                osw.write(text);
                osw.close();
                int exitCode = uncrustifyProcess.waitFor();

                // TODO log this stuff
//                System.out.println("uncrustify exitCode: " + exitCode);
//                BufferedReader er = new BufferedReader(new InputStreamReader(uncrustifyProcess.getErrorStream()));
//                for (String line; (line = er.readLine()) != null;) {
//                    System.out.println(line);
//                }

                if (exitCode != 0) {
                    formattingRequest.onError("Uncrustify formatting failed",
                            "Exit code " + exitCode + ". See logs for more information.");
                }

                InputStream is = uncrustifyProcess.getInputStream();
                ByteArrayOutputStream formattedText = new ByteArrayOutputStream();
                byte[] buffer = new byte[READ_BUF_SIZE];
                for (int length; (length = is.read(buffer)) != -1; ) {
                    formattedText.write(buffer, 0, length);
                }

                formattingRequest.onTextReady(formattedText.toString());
            } catch(IOException | InterruptedException e) {
                System.err.println("error running uncrustify: " + e.getMessage());
                formattingRequest.onError("Uncrustify failed",
                        "Exception occurred while running Uncrustify. See logs for details.");
            }
        }
    }
}
