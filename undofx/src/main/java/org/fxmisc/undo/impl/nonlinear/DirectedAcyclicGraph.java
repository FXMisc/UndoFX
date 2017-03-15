package org.fxmisc.undo.impl.nonlinear;

import org.reactfx.SuspendableNo;

import java.util.List;

public interface DirectedAcyclicGraph<C, T> {

    /**
     * Indicates whether the underlying model is in the process of being modified.
     */
    SuspendableNo performingActionProperty();

    boolean isPerformingAction();

    /******************************************************
     *  Undos                                             *
     ******************************************************/

    /**
     * Given a new change and an undoable change, either returns an updated version of the undoable
     *                    change or the original undoable change if no update is needed
     * @param outdatedUndo an undo stored in a NonlinearChangeQueue
     * @param pushedChange a new change made by some source
     * @return either an updated version of {@code outdatedUndo} or the original one if no update is needed
     */
    C updateUndo(C outdatedUndo, C pushedChange);

    /**
     * Returns true if undo can be undone as is or after being bubbled
     */
    boolean isUndoValid(C undo);

    /**
     * Returns the result of a bubbled undo while factoring in its dependencies. See also {@link BubbledUndoResult}.
     * @param undo the undo that can be bubbled
     */
    BubbledUndoResult<C, T> bubbleUndo(C undo);

    /******************************************************
     *  Redos                                             *
     ******************************************************/

    /**
     * Given a new change and a redoable change, either returns an updated version of the redoable
     *                    change or the original redoable change if no update is needed
     * @param outdatedRedo a redo stored in a {@link NonlinearChangeQueue}
     * @param pushedChange a new change made by some source
     * @return either the original redo or an updated version
     */
    C updateRedo(C outdatedRedo, C pushedChange);

    /**
     * Returns true if redo can be redone as is or after being bubbled
     */
    boolean isRedoValid(C redo);

    /**
     * Returns the {@link BubbledRedoResult} of a bubbled redo.
     * @param redo the undo that can be bubbled
     */
    BubbledRedoResult<C> bubbleRedo(C redo);

    /**
     * Returns true if all registered {@link NonlinearChangeQueue} have been unregistered
     * (see {@link #removeQueue(NonlinearChangeQueue<C>)} and cleans up any used resources.
     */
    boolean close();

    /**
     * Adds the given queue to the graph for dependency tracking, change updating, and the recalculation of
     * the next valid undo and redo. Note: queue's list of changes must be empty to prevent pollution of the graph.
     */
    void addQueue(NonlinearChangeQueue<C> queue);

    /**
     * Stops tracking the given queue's undos and removes the queue from the graph.
     */
    void removeQueue(NonlinearChangeQueue<C> queue);

    /**
     * Updates all queue's changes via {@link NonlinearChangeQueue#updateChangesWithPush(Object)} when a new change
     * has been pushed by a queue.
     *
     * @param pushedChange the new change done to the underlying model
     */
    void updateChangesWithPush(C pushedChange);

    /**
     * Updates all other queues changes via {@link NonlinearChangeQueue#updateChangesWithPush(Object)} but
     * updates {@code source} with {@link NonlinearChangeQueue#updateChangesWithUndo(Object)}.
     *
     * @param source the change queue undoing one of its undoable changes
     * @param undo the change being undone
     */
    void updateChangesWithUndo(NonlinearChangeQueue<C> source, C undo);

    /**
     * Updates all other queues changes via {@link NonlinearChangeQueue#updateChangesWithPush(Object)} but
     * updates {@code source} with {@link NonlinearChangeQueue#updateChangesWithRedo(Object)}.
     *
     * @param source the change queue redoing one of its redoable changes
     * @param redo the change being redone
     */
    void updateChangesWithRedo(NonlinearChangeQueue<C> source, C redo);

    /**
     * When an outdated undo is updated to the updated undo, remaps the list of dependencies from the outdated one
     * to the updated one.
     */
    void remapEdges(C outdatedUndo, C updatedUndo);

    /**
     * When a bubblyUndo is bubbled, remaps any changes the original bubblyUndo modifies to the grounded portion
     * and recursively updates and remaps its dependencies to modify the grounded portion.
     *
     * @param bubblyUndo the bubblyUndo that was bubbled
     * @param result the bubbled result
     */
    void remapBubbledUndoDependencies(C bubblyUndo, BubbledUndoResult<C, T> result);

    /**
     * Adds a {@code pushedChange} to {@code undo}'s list of dependencies if the first modifies the second.
     *
     * @param pushedChange a new change pushed or a redo change that was redone
     * @param undo a change that has already been done
     */
    void addDependencyIfExists(C pushedChange, C undo);

    /**
     * Stops tracking the given change's dependencies, and removes them from other change's list of dependencies.
     * This method should be called sometime during {@link NonlinearChangeQueue#prev()}.
     *
     * @param undos a list of undoable changes that are being undone.
     */
    void forgetUndos(List<C> undos);

    /**
     * Stops tracking the given changes' dependencies, but does not remove them from other change's list of
     * dependencies. This method should be called during {@link NonlinearChangeQueue#forgetHistory()}.
     *
     * @param changes a list of changes that have been done to the underlying model but which are being forgotten
     *                in the queue's history.
     */
    void stopTrackingDependenciesOf(List<C> changes);

    /**
     * True if this change has not been modified by another change.
     */
    boolean isMutuallyIndependent(C change);

}
