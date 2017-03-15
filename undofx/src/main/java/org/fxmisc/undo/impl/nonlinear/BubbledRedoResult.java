package org.fxmisc.undo.impl.nonlinear;

import java.util.Objects;

/**
 * The result of a bubbled redo. Bubbling a redo is always attempted but not always needed. Thus, {@link #wasBubbled()}
 * indicates whether the redo was bubbled. If true, {@link #getGrounded()} and {@link #getBubbled()} will return
 * non-null values. Otherwise, these will be null.
 *
 * @param <C> type of change
 */
public final class BubbledRedoResult<C> extends BubbledResult<C> {

    private BubbledRedoResult(C grounded, C bubbled) {
        super(grounded, bubbled);
    }

    /** True if the redo was bubbled. */
    public boolean wasBubbled() { return getGrounded() != null && getBubbled() != null; }

    public static <C> BubbledRedoResult<C> withBubble(C grounded, C bubbled) {
        return new BubbledRedoResult<C>(grounded, bubbled);
    }

    public static <C> BubbledRedoResult<C> noBubble() {
        return new BubbledRedoResult<C>(null, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof BubbledRedoResult<?>)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        BubbledRedoResult<C> that = (BubbledRedoResult<C>) obj;
        return Objects.equals(this.getBubbled(), that.getBubbled())
                && Objects.equals(this.getGrounded(), that.getGrounded());
    }

    @Override
    public int hashCode() {
        int result = 31 * Objects.hashCode(getBubbled());
        result = 31 * result + Objects.hashCode(getGrounded());
        return result;
    }

    @Override
    public String toString() {
        return String.format("BubbledRedoResult(bubbled=%s grounded=%s wasBubbled=%s)",
                getBubbled(), getGrounded(), wasBubbled());
    }
}
