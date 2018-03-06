package org.fxmisc.undo;

import javafx.beans.value.ObservableBooleanValue;

import org.reactfx.value.Val;

public interface UndoManager<C> {

    /**
     * Represents a position in UndoManager's history.
     */
    interface UndoPosition {
        /**
         * Sets the mark of the underlying UndoManager at this position.
         * The mark is set whether or not this position is valid. It is
         * OK for an UndoManager to be marked at an invalid position.
         */
        void mark();

        /**
         * Checks whether this history position is still valid.
         * A position becomes invalid when
         * <ul>
         *   <li>the change immediately preceding the position is undone
         *   and then discarded due to another incoming change; or</li>
         *   <li>the change immediately following the position is forgotten
         *   due to history size limit.</li>
         * </ul>
         */
        boolean isValid();
    }

    /**
     * Undo the most recent change, if there is any change to undo.
     * @return {@code true} if a change was undone, {@code false} otherwise.
     */
    boolean undo();

    /**
     * Redo previously undone change, if there is any change to redo.
     * @return {@code true} if a change was redone, {@code false} otherwise.
     */
    boolean redo();

    /**
     * Indicates whether there is a change that can be undone.
     */
    Val<Boolean> undoAvailableProperty();
    boolean isUndoAvailable();

    /**
     * Gives a peek at the change that will be undone by {@link #undo()}.
     */
    Val<C> nextUndoProperty();
    default C getNextUndo() { return nextUndoProperty().getValue(); }

    /**
     * Gives a peek at the change that will be redone by {@link #redo()}.
     */
    Val<C> nextRedoProperty();
    default C getNextRedo() { return nextRedoProperty().getValue(); }

    /**
     * Indicates whether there is a change that can be redone.
     */
    Val<Boolean> redoAvailableProperty();
    boolean isRedoAvailable();

    /**
     * Indicates whether this undo manager is currently performing undo or redo
     * action.
     */
    ObservableBooleanValue performingActionProperty();
    boolean isPerformingAction();

    /**
     * Prevents the next change from being merged with the latest one.
     */
    void preventMerge();

    /**
     * Forgets all changes prior to the current position in the history.
     */
    void forgetHistory();

    /**
     * Returns the current position within this UndoManager's history.
     */
    UndoPosition getCurrentPosition();

    /**
     * Sets this UndoManager's mark to the current position.
     * This method is a convenient shortcut for
     * {@code getCurrentPosition().mark()}.
     */
    default void mark() {
        getCurrentPosition().mark();
    }

    /**
     * Indicates whether this UndoManager's current position within
     * its history is the same as the last marked position.
     */
    ObservableBooleanValue atMarkedPositionProperty();
    boolean isAtMarkedPosition();

    /**
     * Stops observing change events.
     */
    void close();
}
