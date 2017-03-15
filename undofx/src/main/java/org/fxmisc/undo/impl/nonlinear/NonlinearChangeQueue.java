package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueue;

import java.util.List;

public interface NonlinearChangeQueue<C> extends ChangeQueue<C> {

    /**
     * Returns an unmodifiable view of the queue's changes
     */
    List<C> getChanges();

    /**
     * Returns an unmodifiable view of the queue's undos
     */
    List<C> getUndoChanges();

    /**
     * Removes this queue from its {@link DirectedAcyclicGraph}
     */
    void close();

    void push(C newOrMergedChange);

    void push(C undo, C newChange);

    /**
     * Recalculates this queue's next valid undo and redo if one exists. A change is valid if it can be undone/redone
     * immediately or only after it has been bubbled.
     */
    void recalculateValidChanges();

    void updateChangesWithPush(C pushedChange);

    /**
     * Notifies the underlying {@link org.fxmisc.undo.impl.nonlinear.DirectedAcyclicGraph} that this queue has undone one of its
     * undos and that all other queues changes should be updated to account for that
     */
    void updateGraphWithUndo(C undo);

    /**
     * Notifies the underlying {@link org.fxmisc.undo.impl.nonlinear.DirectedAcyclicGraph} that this queue has redone one of its
     * redos and that all other queues changes should be updated to account for that
     */
    void updateGraphWithRedo(C redo);

    /**
     * When this queue undoes one of its undos that is already stored in this queue,
     * updates this queue's undoes (minus this undo) and other redos in light of the change of the underlying model
     */
    void updateChangesWithUndo(C undo);

    /**
     * When this queue redoes one of its redos that is already stored in this queue,
     * updates this queue's undos and other redos in light of the change of the underlying model.
     */
    void updateChangesWithRedo(C redo);

}
