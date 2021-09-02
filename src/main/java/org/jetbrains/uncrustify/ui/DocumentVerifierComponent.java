package org.jetbrains.uncrustify.ui;

import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

abstract public class DocumentVerifierComponent extends HyperlinkLabel {
    public static final String DOCUMENT_VALID = "documentValid";

    private boolean myCheckedPathValid;
    private final @NotNull Document myCheckedDocument;

    public DocumentVerifierComponent(@NotNull Document document) {
        super();
        myCheckedDocument = document;
        document.addDocumentListener(new DocumentVerifierAdapter());
        verifyDocument();
    }

    protected void setValid(boolean b) {
        boolean old = myCheckedPathValid;
        myCheckedPathValid = b;
        firePropertyChange(DOCUMENT_VALID, old, myCheckedPathValid);
    }

    protected @NotNull Document getDocument() {
        return myCheckedDocument;
    }

    public boolean isDocumentValid() {
        return myCheckedPathValid;
    }

    public void setDocumentIsBeingChecked() {
        setValid(false);
        setText("");
        setIcon(AnimatedIcon.Default.INSTANCE);
    }

    abstract public void verifyDocument();

    public class DocumentVerifierAdapter extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            verifyDocument();
        }
    }
}
