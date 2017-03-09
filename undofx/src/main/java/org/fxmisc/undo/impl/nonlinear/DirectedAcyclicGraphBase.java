package org.fxmisc.undo.impl.nonlinear;

import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class DirectedAcyclicGraphBase<C, T> implements DirectedAcyclicGraph<C, T> {

    private final SuspendableNo performingAction = new SuspendableNo();
    public final SuspendableNo performingActionProperty() { return performingAction; }
    public final boolean isPerformingAction() { return performingAction.get(); }

    private final List<NonlinearChangeQueue<C>> queues = new ArrayList<>(1);

    /**
     * The changes (values: from) that modify one change (key: to).
     *
     * <pre>
     *            --- (Dependency: From) \
     *           /                       |
     * (Key: To) <--- (Dependency: From) |- {@code Value: List<C>}
     *           \                       |
     *            --- (Dependency: From) /
     * </pre>
     *
     */
    private final Map<C, List<C>> toFromEdges = new HashMap<>();
    private final Subscription subscription;

    /**
     * returns true if the given undo is modified/altered by the pushed change
     */
    protected abstract boolean firstIsModifiedBySecond(C undo, C pushedChange);

    // Undos
    public abstract C updateUndo(C outdatedUndo, C pushedChange);
    public abstract boolean isUndoValid(C undo);
    protected abstract BubbledUndoResult<C, T> doBubbleUndo(C bubblyUndo, List<C> dependencies);
    public abstract C updateBubbledUndoDependency(C dependency, C bubblyUndo, BubbledUndoResult<C, T> result);
    public abstract C updateDependency(C dependency, C oldChange, C newChange);

    // Redos
    public abstract C updateRedo(C outdatedRedo, C pushedChange);
    public abstract boolean isRedoValid(C redo);
    public abstract BubbledRedoResult<C> bubbleRedo(C redo);

    public DirectedAcyclicGraphBase() {
        subscription = performingAction.values()
                .filter(performingUpdate -> !performingUpdate && !queues.isEmpty())
                .subscribe(ignore -> recalculateAllValidChanges());
    }

    public final boolean close() {
        if (queues.size() == 0) {
            subscription.unsubscribe();
            return true;
        } else {
            return false;
        }
    }

    public final void addQueue(NonlinearChangeQueue<C> queue) {
        if (!queue.getChanges().isEmpty()) {
            throw new IllegalArgumentException("A NonlinearChangeQueue cannot be registered if it already has 1 or more changes.");
        }
        queues.add(queue);
    }

    public final void removeQueue(NonlinearChangeQueue<C> queue) {
        stopTrackingDependenciesOf(queue.getUndoChanges());
        queues.remove(queue);
    }

    public final BubbledUndoResult<C, T> bubbleUndo(C change) {
        List<C> dependencies = toFromEdges.containsKey(change) ? toFromEdges.get(change) : Collections.emptyList();
        return doBubbleUndo(change, dependencies);
    }

    public final void updateChangesWithPush(C pushedChange) {
        queues.forEach(q -> q.updateChangesWithPush(pushedChange));
    }

    public final void updateChangesWithUndo(NonlinearChangeQueue<C> source, C undo) {
        queues.forEach(q -> {
            if (q == source) {
                q.updateChangesWithUndo(undo);
            } else {
                q.updateChangesWithPush(undo);
            }
        });
    }

    public final void updateChangesWithRedo(NonlinearChangeQueue<C> source, C redo) {
        queues.forEach(q -> {
            if (q == source) {
                q.updateChangesWithRedo(redo);
            } else {
                q.updateChangesWithPush(redo);
            }
        });
    }

    /**
     * Recalculates all {@link #addQueue(NonlinearChangeQueue) added queues}' next valid undo change and redo change.
     */
    private void recalculateAllValidChanges() {
        queues.forEach(NonlinearChangeQueue::recalculateValidChanges);
    }

    public final void remapEdges(C outdatedUndo, C updatedUndo) {
        updateEarlierChangeDependency(outdatedUndo, updatedUndo);

        if (toFromEdges.containsKey(outdatedUndo)) {
            toFromEdges.put(updatedUndo, toFromEdges.remove(outdatedUndo));
        }
    }

    /** OutdatedDependency modifies an earlier change. When it gets updated, that change's list of dependencies
     * must be updated to point to the updated version of the dependency. */
    private void updateEarlierChangeDependency(C outdatedDependency, C updatedDependency) {
        toFromEdges.keySet().stream()
                .filter(key -> key != outdatedDependency)
                .map(toFromEdges::get)
                .forEach(dependencies -> {
                    int index = dependencies.indexOf(outdatedDependency);
                    if (index != -1) {
                        dependencies.set(index, updatedDependency);
                    }
                });
    }

    /**
     * (Depth-first) Updates the bubbled undo's dependencies using
     * {@link #updateBubbledUndoDependency(Object, Object, BubbledUndoResult)} before updating those dependencies'
     * dependencies, if any, with {@link #updateDependency(Object, Object, Object)}.
     */
    public final void remapBubbledUndoDependencies(C bubblyUndo, BubbledUndoResult<C, T> result) {
        updateEarlierChangeDependency(bubblyUndo, result.getGrounded());
        updateRemapDependenciesRecursively(bubblyUndo, result.getGrounded(),
                dependency -> updateBubbledUndoDependency(dependency, bubblyUndo, result));
    }

    /** (Depth-first) Recursively updates all dependencies of the dependencies of a bubbled undo */
    private void updateRemapDependencyOfDependency(C oldChange, C newChange) {
        updateRemapDependenciesRecursively(oldChange, newChange,
                outdatedDep -> updateDependency(outdatedDep, oldChange, newChange));
    }

    /** (Depth-first) Recursively updates all oldChange's dependencies and remaps them to newChange */
    private void updateRemapDependenciesRecursively(C oldChange, C newChange, Function<C, C> depUpdater) {
        List<C> dependencies = toFromEdges.remove(oldChange);
        for (int i = 0; i < dependencies.size(); i++) {
            C outdatedDep = dependencies.get(i);
            C updatedDep = depUpdater.apply(outdatedDep);
            if (!updatedDep.equals(outdatedDep)) {
                updateRemapDependencyOfDependency(outdatedDep, updatedDep);
                dependencies.set(i, updatedDep);
            }
        }
        toFromEdges.put(newChange, dependencies);
    }

    public final void addDependencyIfExists(C pushedChange, C undo) {
        if (firstIsModifiedBySecond(undo, pushedChange)) {
            addEdgeFromTo(pushedChange, undo);
        }
    }

    private void addEdgeFromTo(C from, C to) {
        if (toFromEdges.containsKey(to)) {
            toFromEdges.get(to).add(from);
        } else {
            List<C> list = new ArrayList<>(1);
            list.add(from);
            toFromEdges.put(to, list);
        }
    }

    public final void forgetUndos(List<C> undos) {
        if (toFromEdges.isEmpty()) {
            return;
        }

        List<C> keysToRemove = new ArrayList<>(undos);
        toFromEdges.keySet().stream()
                .filter(key -> !undos.contains(key))
                .forEach(key -> {
                    List<C> dependencies = toFromEdges.get(key);
                    dependencies.removeAll(undos);
                    if (dependencies.isEmpty()) {
                        keysToRemove.add(key);
                    }});
        keysToRemove.forEach(toFromEdges::remove);
    }

    public final void stopTrackingDependenciesOf(List<C> changes) {
        changes.forEach(toFromEdges::remove);
    }

    public final boolean isMutuallyIndependent(C change) {
        return !toFromEdges.containsKey(change);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DirectedAcyclicGraphBase(");

        sb.append("\n\ttoFromEdges=[");
        for (C key : toFromEdges.keySet()) {
            sb.append("\n\t\tChange (").append(key).append(") is modified by [").append(toFromEdges.get(key)).append("]");
        }
        sb.append("\n\t]");

        sb.append("\n\tqueues=[\n");
        for (NonlinearChangeQueue<C> q : queues) {
            sb.append("  ").append(q.toString()).append("\n");
        }
        sb.append("])");
        return sb.toString();
    }
}