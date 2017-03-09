package org.fxmisc.richtextfx;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Guard;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class DocumentModel {

    private Var<String> internalText;

    private SuspendableVal<String> text;
    public final String getText() { return text.getValue(); }
    public final Val<String> textProperty() { return text; }

    public final String getText(int start) {
        start = clamp(start);
        return getText().substring(start);
    }

    public final String getText(int start, int end) {
        start = clamp(start);
        end = clamp(end);

        return getText().substring(start, end);
    }

    private Val<Integer> length;
    public final int getLength() { return length.getValue(); }
    public final Val<Integer> lengthProperty() { return length; }

    private EventSource<TextChange> changeStream = new EventSource<>();
    public EventStream<TextChange> changes() { return changeStream; }

    private boolean emitChanges;

    public DocumentModel() {
        this(true);
    }

    public DocumentModel(boolean emitChanges) {
        internalText = Var.newSimpleVar("");
        text = internalText.suspendable();
        length = text.map(String::length);
        this.emitChanges = emitChanges;
    }

    public void clearText() {
        replaceText("");
    }

    public void delete(int from, int to) {
        replace(from, to, "");
    }

    public void replace(TextChange change) {
        replace(change.getStart(), change.removedEndPosition(), change.getInserted());
    }

    public void replaceText(String replacement) {
        replace(0, getLength(), replacement);
    }

    public void replace(int start, int end, String replacement) {
        start = clamp(start);
        end = clamp(end);

        checkArguments(start, end);

        TextChange change = null;
        try (Guard g = text.suspend()) {
            String outdatedText = internalText.getValue();

            String left      = outdatedText.substring(0, start);
            String oldMiddle = outdatedText.substring(start, end);
            String right     = outdatedText.substring(end);

            internalText.setValue(left + replacement + right);

            if (!replacement.isEmpty() || !oldMiddle.isEmpty()) {
                change = new TextChange(start, oldMiddle, replacement);
            }
        }
        if (emitChanges && change != null) {
            changeStream.push(change);
        }
    }

    private int clamp(int index) {
        return Math.max(0, Math.min(index, getLength()));
    }

    private void checkArguments(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("'start' must be less than or equal to 'end': start=" + start + " end=" + end);
        }
    }

    @Override
    public String toString() {
        return String.format("DocumentModel(length=%s text=%s", getLength(), getText());
    }
}
