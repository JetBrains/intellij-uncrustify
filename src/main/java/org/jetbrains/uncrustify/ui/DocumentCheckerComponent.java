package org.jetbrains.uncrustify.ui;

import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

abstract public class DocumentCheckerComponent extends SimpleColoredComponent {
    public static final String EXECUTABLE_VALID_PROPERTY = "uncrustifyVersionValid";

    private boolean myCheckedPathValid;
    private final @NotNull Document myCheckedDocument;

    public DocumentCheckerComponent(@NotNull Document document) {
        super();
        myCheckedDocument = document;
        document.addDocumentListener(new PathToExecutableAdapter());
        checkDocument();
    }

    private void setValid(boolean b) {
        boolean changed = myCheckedPathValid != b;
        myCheckedPathValid = b;
        if (changed) {
            firePropertyChange(EXECUTABLE_VALID_PROPERTY, !myCheckedPathValid, myCheckedPathValid);
        }
    }

    protected @NotNull Document getDocument() {
        return myCheckedDocument;
    }

    public boolean isPathValid() {
        return myCheckedPathValid;
    }

    public void setPathIsEmpty(String message) {
        setValid(false);
    }

    public void setPathIsValid(String message) {
        setValid(true);
    }

    public void setPathIsInvalid(String message) {
        setValid(false);
    }

    public void setPathIsBeingChecked(String message) {
        setValid(false);
        clear();
        setIcon(AnimatedIcon.Default.INSTANCE);
    }

    abstract public void checkDocument();

    public class PathToExecutableAdapter extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            checkDocument();
        }
    }
}
