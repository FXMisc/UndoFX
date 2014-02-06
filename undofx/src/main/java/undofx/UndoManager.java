package undofx;

import javafx.beans.value.ObservableBooleanValue;

public interface UndoManager {
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
    ObservableBooleanValue undoAvailableProperty();
    boolean isUndoAvailable();

    /**
     * Indicates whether there is a change that can be redone.
     */
    ObservableBooleanValue redoAvailableProperty();
    boolean isRedoAvailable();

    /**
     * Prevents the next change from being merged with the latest one.
     */
    void preventMerge();

    /**
     * Stops observing change events.
     */
    void close();
}
