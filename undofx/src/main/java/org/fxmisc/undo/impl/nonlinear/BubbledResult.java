package org.fxmisc.undo.impl.nonlinear;

import java.util.Objects;

/**
 * A BubbledResult contains the result of splitting a change into two smaller parts: the {@code grounded} change
 * (the change that should replace the original change as later changes have modified this part of the original change)
 * and the {@code bubbled} change (the change that can be undone/redone due to later changes not modifying it in
 * any way.)
 *
 * <p>Note: Sometimes, it is not known whether a valid redo needs to be bubbled for it to be in its valid form or not.
 * In cases like these, it is bubbled irregardless. If such a redo doesn't need to be bubbled, it is assumed that the
 * original redo will equal the {@code bubbled} change and the {@code grounded} change will be null.
 * In other words, {@code original.equals(bubbledResult.getBubbled())} will return true).
 * @param <C> parameter for change
 */
public class BubbledResult<C> {

    private final C bubbled;
    public final C getBubbled() { return bubbled; }

    private final C grounded;
    public final C getGrounded() { return grounded; }

    public BubbledResult(C bubbled, C grounded) {
        this.bubbled = bubbled;
        this.grounded = grounded;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof BubbledResult<?>)) {
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