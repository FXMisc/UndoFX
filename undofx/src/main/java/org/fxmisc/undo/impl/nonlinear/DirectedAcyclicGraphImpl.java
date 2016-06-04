package org.fxmisc.undo.impl.nonlinear;

import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;
import org.reactfx.util.TriFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class DirectedAcyclicGraphImpl<C> implements DirectedAcyclicGraph<C> {

    private final SuspendableNo performingAction = new SuspendableNo();
    public final SuspendableNo performingActionProperty() { return performingAction; }
    public final boolean isPerformingAction() { return performingAction.get(); }

    private final List<NonLinearChangeQueue<C>> queues = new ArrayList<>(1);
    private final HashMap<C, List<C>> toFromEdges = new HashMap<>();
    private final Subscription subscription;

    private final BiPredicate<C, C> firstDependsOnSecond;

    // Undos Functional Programming
    private final BiFunction<C, C, C> undoUpdater;
    public final BiFunction<C, C, C> getUndoUpdater() { return undoUpdater; }

    private final Predicate<C> isValidUndo;
    public final Predicate<C> getIsValidUndo() { return isValidUndo; }

    private final BiFunction<C, List<C>, BubbledResult<C>> undoBubbler;

    private final TriFunction<C, C, BubbledResult<C>, C> undoUpdaterPostBubble;
    public final TriFunction<C, C, BubbledResult<C>, C> getUndoUpdaterPostBubble() { return undoUpdaterPostBubble; }

    // Redos Functional Programming
    private final BiFunction<C, C, C> redoUpdater;
    public final BiFunction<C, C, C> getRedoUpdater() { return redoUpdater; }

    private final Predicate<C> isValidRedo;
    public final Predicate<C> getIsValidRedo() { return isValidRedo; }

    private final Function<C, BubbledResult<C>> redoBubbler;

    private final TriFunction<C ,C, BubbledResult<C>, C> redoUpdaterPostBubble;
    public final TriFunction<C ,C, BubbledResult<C>, C> getRedoUpdaterPostBubble() { return redoUpdaterPostBubble; }

    /**
     *
     * @param firstDependsOnSecond returns true if the first given change modifies/alters the second given change
     * @param undoUpdater Given a new change and an undoable change, either returns an updated version of the undoable
     *                    change or the original undoable change if no update is needed
     * @param isValidUndo returns true if the given undo is mutually independent or could be bubbled
     * @param undoBubbler bubbles a "bubbly" undo. See also {@link BubbledResult}.
     * @param undoUpdaterPostBubble updates all changes after a "bubbly" undo has been bubbled to refactor the
     *                                    result's grounded change and to ignore the result's bubbled change
     * @param redoUpdater Given a new change and an redoable change, either returns an updated version of the redoable
     *                    change or the original redoable change if no update is needed
     * @param isValidRedo returns true if the given redo is redoable in whole without bubbling it or in part after
     *                    bubbling it
     * @param redoBubbler bubbles a "bubbly" redo. See also {@link BubbledResult}
     * @param redoUpdaterPostBubble updates all redos after a "bubbly" undo/redo has been bubbled
     */
    public DirectedAcyclicGraphImpl(
            BiPredicate<C, C> firstDependsOnSecond,
            BiFunction<C, C, C> undoUpdater,
            Predicate<C> isValidUndo,
            BiFunction<C, List<C>, BubbledResult<C>> undoBubbler,
            TriFunction<C, C, BubbledResult<C>, C> undoUpdaterPostBubble,
            BiFunction<C, C, C> redoUpdater,
            Predicate<C> isValidRedo,
            Function<C, BubbledResult<C>> redoBubbler,
            TriFunction<C ,C, BubbledResult<C>, C> redoUpdaterPostBubble) {
        this.firstDependsOnSecond = firstDependsOnSecond;

        this.undoUpdater = undoUpdater;
        this.isValidUndo = isValidUndo;
        this.undoBubbler = undoBubbler;
        this.undoUpdaterPostBubble = undoUpdaterPostBubble;

        this.redoUpdater = redoUpdater;
        this.isValidRedo = isValidRedo;
        this.redoBubbler = redoBubbler;
        this.redoUpdaterPostBubble = redoUpdaterPostBubble;

        subscription = performingAction.values()
                .filter(perfomingUpdate -> !perfomingUpdate)
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

    public final void registerQueue(NonLinearChangeQueue<C> queue) {
        if (!queue.getChanges().isEmpty()) {
            throw new IllegalArgumentException("A ChangeQueue cannot be registered if it already has 1 or more changes.");
        }
        queues.add(queue);
    }

    public final void unregisterQueue(NonLinearChangeQueue<C> queue) {
        forget(queue.getChanges());
        queues.remove(queue);
    }

    private NonLinearChangeQueue<C> latestChangeSource;
    public final void setLatestChangeSource(NonLinearChangeQueue<C> source) {
        latestChangeSource = source;
    }
    public final NonLinearChangeQueue<C> getLatestChangeSource() {
        return latestChangeSource;
    }

    public final BubbledResult<C> bubbleRedo(C change) {
        return redoBubbler.apply(change);
    }

    public final BubbledResult<C> bubbleUndo(C change) {
        List<C> dependencies = toFromEdges.get(change);
        return undoBubbler.apply(change, dependencies);
    }

    public final void updateChangesWithPush(C pushedChange) {
        queues.forEach(q -> q.updateChangesWithPush(pushedChange));
    }

    public final void updateChangesWithRedo(NonLinearChangeQueue<C> source, C redo) {
        queues.forEach(q -> {
            if (q == source) {
                q.updateChangesWithRedo(redo);
            } else {
                q.updateChangesWithPush(redo);
            }
        });
    }

    public final void updateChangesPostUndoBubble(C original, BubbledResult<C> bubbledResult) {
        queues.forEach(q -> q.updateChangesPostUndoBubble(original, bubbledResult));
    }

    public final void updateRedosPostRedoBubble(C original, BubbledResult<C> bubbledResult) {
        queues.forEach(q -> q.updateRedosPostChangeBubble(original, bubbledResult));
    }

    public final void recalculateAllValidChanges() {
        queues.forEach(NonLinearChangeQueue::recalculateValidChanges);
    }

    public final void remapEdges(C outdated, C updated) {
        toFromEdges.forEach((key, list) -> {
            if (key.equals(outdated)) {
                toFromEdges.put(updated, toFromEdges.remove(outdated));
            } else {
                int index = list.indexOf(outdated);
                if (index != -1) {
                    list.set(index, updated);
                }
            }
        });
    }

    public final void testForDependency(C from, C to) {
        if (firstDependsOnSecond.test(from, to)) {
            addEdgeFromTo(from, to);
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

    public final void forget(List<C> changes) {
        changes.forEach(toFromEdges::remove);
        toFromEdges.values().forEach(ls -> ls.removeAll(changes));
    }

    public final void removeRelatedEdgesOf(C change) {
        toFromEdges.forEach((key, list) -> {
            if (key.equals(change)) {
                toFromEdges.remove(key);
            } else {
                list.remove(change);
            }
        });
    }

    public final boolean isMutuallyIndependent(C change) {
        return toFromEdges.get(change).isEmpty();
    }

}
