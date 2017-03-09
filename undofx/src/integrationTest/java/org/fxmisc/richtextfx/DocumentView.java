package org.fxmisc.richtextfx;

import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;
import org.reactfx.SuspendableNo;

public class DocumentView {

    private final DocumentModel doc;

    private UndoManager undoManager;
    public final UndoManager getUndoManager() { return undoManager; }
    public final void setUndoManager(UndoManager undoManager) {
        if (this.undoManager != null) {
            this.undoManager.close();
        }
        this.undoManager = undoManager;
    }

    private final SuspendableNo allowChange = new SuspendableNo();
    private final EventStream<TextChange> changesDoneByThisView;
    public final EventStream<TextChange> changesDoneByThisViewEvents() { return changesDoneByThisView; }

    public DocumentView(DocumentModel doc) {
        this.doc = doc;
        changesDoneByThisView = doc.changes().conditionOn(allowChange);
    }

    public void replace(TextChange change) {
        replace(change.getStart(), change.removedEndPosition(), change.getInserted());
    }

    /** Replaces entire text with the given replacement */
    public void replaceText(String replacement) {
        replace(0, doc.getLength(), replacement);
    }

    /** Replaces portion of text with given replacement */
    public void replace(int start, int end, String replacement) {
        allowChange.suspendWhile(() -> doc.replace(start, end, replacement));
    }

    /** Prepends the text while optionally preventing any changes from being merged */
    public void prependText(boolean preventMerge, String text) {
        if (preventMerge) {
            getUndoManager().preventMerge();
        }
        replace(0, 0, text);
    }

    /** Prepends the text and allows the resulting TextChange to merge with previous one if applicable */
    public void prependText(String text) {
        prependText(false, text);
    }

    /** {@link #prependText(boolean, String)} with the given texts */
    public int prependTexts(boolean preventMerge, String... texts) {
        for (String t : texts) {
            prependText(preventMerge, t);
        }
        return texts.length;
    }

    /** {@link #prependText(String)} with the given texts */
    public int prependTexts(String... texts) {
        return prependTexts(false, texts);
    }

    /** Appends the text while optionally preventing any changes from being berged */
    public void appendText(boolean preventMerge, String text) {
        if (preventMerge) {
            getUndoManager().preventMerge();
        }
        replace(doc.getLength(), doc.getLength(), text);
    }

    /** Appends the text and allows resulting text change to merge with previous one if applicable*/
    public void appendText(String text) {
        appendText(false, text);
    }

    public int appendTexts(boolean preventMerge, String... texts) {
        for (String t : texts) {
            appendText(preventMerge, t);
        }
        return texts.length;
    }

    /** {@link #appendText(String)} with the given texts */
    public int appendTexts(String... texts) {
        return appendTexts(false, texts);
    }

    public void undo() {
        undoManager.undo();
    }

    public boolean isUndoAvailable() {
        return undoManager.isUndoAvailable();
    }

    public void redo() {
        undoManager.redo();
    }

    public boolean isRedoAvailable() {
        return undoManager.isRedoAvailable();
    }
}
