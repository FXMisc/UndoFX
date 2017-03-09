package org.fxmisc.undo.impl.nonlinear;

import java.util.Objects;

/**
 * A BubbledResult contains the result of splitting a change into two smaller parts: the {@code grounded} change
 * (the change that should replace the original change as later changes have modified this part of the original change)
 * and the {@code bubbled} change (the change that can be undone/redone due to later changes not modifying it in
 * any way.)
 *
 * @param <C> type for change
 */
class BubbledResult<C> {

    private final C grounded;
    public final C getGrounded() { return grounded; }

    private final C bubbled;
    public final C getBubbled() { return bubbled; }

    BubbledResult(C grounded, C bubbled) {
        this.grounded = grounded;
        this.bubbled = bubbled;
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
        BubbledResult<C> that = (BubbledResult<C>) obj;
        return Objects.equals(this.bubbled, that.bubbled)
                && Objects.equals(this.grounded, that.grounded);
    }

    @Override
    public int hashCode() {
        int result = 31 * bubbled.hashCode();
        result = 31 * result + grounded.hashCode();
        return result;
    }
}
