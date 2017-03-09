package org.fxmisc.undo.impl.nonlinear;

import java.util.Objects;

/**
 * The result of an undo bubble.
 * <p>Note: Sometimes, it is not known whether a valid redo needs to be bubbled for it to be in its valid form or not.
 * In cases like these, it is bubbled irregardless. If such a redo doesn't need to be bubbled, it is assumed that the
 * original redo will equal the {@code bubbled} change and the {@code grounded} change will be null.
 * In other words, {@code original.equals(bubbledResult.getBubbled())} will return true).
 *
 * @param <C> parameter for change
 * @param <T> parameter for object storing any other pertinent information about the bubble
 */
public final class BubbledUndoResult<C, T> extends BubbledResult<C> {

    private final T info;
    public final T getInfo() { return info; }

    public BubbledUndoResult(C grounded, C bubbled, T info) {
        super(grounded, bubbled);
        this.info = info;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof BubbledUndoResult<?, ?>)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        BubbledUndoResult<C, T> that = (BubbledUndoResult<C, T>) obj;
        return Objects.equals(this.getBubbled(), that.getBubbled())
                && Objects.equals(this.getGrounded(), that.getGrounded())
                && Objects.equals(this.info, that.info);
    }

    @Override
    public int hashCode() {
        int result = 31 * getBubbled().hashCode();
        result = 31 * result + getGrounded().hashCode();
        result = 31 * result + info.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("BubbledUndoResult(grounded=%s bubbled=%s info=%s)", getGrounded(), getBubbled(), info);
    }
}