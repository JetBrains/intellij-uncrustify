package org.jetbrains.uncrustify;

import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.editor.DiffVirtualFile;
import com.intellij.diff.editor.SimpleDiffVirtualFile;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.formatting.FormatTextRanges;
import com.intellij.formatting.service.CoreFormattingService;
import com.intellij.formatting.service.FormattingService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class UncrustifyDebugAction extends AnAction {
    private static final boolean enabled = System.getProperties()
            .getProperty("uncrustify.devTools", "false")
            .equalsIgnoreCase("true");

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        if (!enabled) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        List<FormattingService> serviceList = FormattingService.EP_NAME.getExtensionList();
        Optional<FormattingService> uncrustifyService = serviceList.stream().filter((s) -> s instanceof UncrustifyAsyncFormattingService).findFirst();
        if (uncrustifyService.isEmpty()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        e.getPresentation().setVisible(true);

        PsiFile currentFile = getCurrentlySelectedFile(e);
        e.getPresentation().setEnabled(currentFile != null && uncrustifyService.get().canFormat(currentFile));
    }

    private @Nullable PsiFile getCurrentlySelectedFile(@NotNull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return null;
        }

        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor == null) {
            return null;
        }

        return PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<FormattingService> serviceList = FormattingService.EP_NAME.getExtensionList();

        FormattingService coreService = serviceList.stream().filter((s) -> s instanceof CoreFormattingService).findFirst().orElse(null);
        FormattingService uncrustifyService = serviceList.stream().filter((s) -> s instanceof UncrustifyAsyncFormattingService).findFirst().orElse(null);
        if (coreService == null|| uncrustifyService == null) {
            return;
        }

        DataContext dataContext = e.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return;
        }

        PsiFile fileToFormat = getCurrentlySelectedFile(e);
        if (fileToFormat == null) {
            return;
        }
        String fileToFormatExtension = FileUtilRt.getExtension(fileToFormat.getName());

        String textToFormat = Objects.requireNonNull(PsiDocumentManager.getInstance(project).getDocument(fileToFormat)).getText();

        VirtualFile coreFormattedFile = new LightVirtualFile("ij." + fileToFormatExtension, fileToFormat.getLanguage(), textToFormat);
        VirtualFile uncrustifyFormattedFile = new LightVirtualFile("uncrustify." + fileToFormatExtension, fileToFormat.getLanguage(), textToFormat);

        PsiFile uffPsi = PsiManager.getInstance(project).findFile(uncrustifyFormattedFile);
        PsiFile cffPsi = PsiManager.getInstance(project).findFile(coreFormattedFile);

        if (uffPsi == null || cffPsi == null) {
            return;
        }

        WriteCommandAction.writeCommandAction(project).run(() -> coreService.formatRanges(cffPsi, new FormatTextRanges(cffPsi.getTextRange(), true), false, false));
        WriteCommandAction.writeCommandAction(project).run(() -> uncrustifyService.formatRanges(uffPsi, new FormatTextRanges(uffPsi.getTextRange(), true), false, false));

        DiffRequest diffRequest = DiffRequestFactory.getInstance().createFromFiles(project, coreFormattedFile, uncrustifyFormattedFile);
        DiffVirtualFile dvf = new SimpleDiffVirtualFile(diffRequest);
        FileEditorManager.getInstance(project).openFile(dvf, true);
    }
}
