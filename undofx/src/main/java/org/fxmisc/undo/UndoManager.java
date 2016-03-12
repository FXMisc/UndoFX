package org.fxmisc.undo;

import javafx.beans.value.ObservableBooleanValue;

public interface UndoManager<Source> {

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
    boolean undo(Source src);

    /**
     * Redo previously undone change, if there is any change to redo.
     * @return {@code true} if a change was redone, {@code false} otherwise.
     */
    boolean redo(Source src);

    /**
     * Indicates whether there is a change that can be undone.
     */
    ObservableBooleanValue undoAvailableProperty(Source src);
    boolean isUndoAvailable(Source src);

    /**
     * Indicates whether there is a change that can be redone.
     */
    ObservableBooleanValue redoAvailableProperty(Source src);
    boolean isRedoAvailable(Source src);

    /**
     * Indicates whether this undo manager is currently performing undo or redo
     * action.
     */
    ObservableBooleanValue performingActionProperty();
    boolean isPerformingAction();

    /**
     * Prevents the next change from being merged with the latest one.
     */
    void preventMerge(Source src);

    /**
     * Forgets all changes prior to the current position in the history.
     */
    void forgetHistory(Source src);

    /**
     * Returns the current position within this UndoManager's history.
     */
    UndoPosition getCurrentPosition(Source src);

    /**
     * Sets this UndoManager's mark to the current position.
     * This method is a convenient shortcut for
     * {@code getCurrentPosition(Source).mark()}.
     */
    void mark(Source src);

    /**
     * Indicates whether this UndoManager's current position within
     * its history is the same as the last marked position.
     */
    ObservableBooleanValue atMarkedPositionProperty(Source src);
    boolean isAtMarkedPosition(Source src);

    /**
     * Stops observing change events only for the given source in a Non-Linear UndoManager
     */
    void close(Source src);

    /**
     * Stops observing all change events.
     */
    void close();
}
