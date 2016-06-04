package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueueBase;
import org.reactfx.SuspendableNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class UnlimitedNonLinearChangeQueue<C> extends ChangeQueueBase<C> implements NonLinearChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final C change;

        QueuePositionImpl(C change) { this.change = change; }

        @Override public boolean isValid() { return changes.contains(change); }

        @Override
        public boolean equals(Object other) {
            if(other instanceof UnlimitedNonLinearChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && change == otherPos.change;
            } else {
                return false;
            }
        }

        private UnlimitedNonLinearChangeQueue<C> getQueue() { return UnlimitedNonLinearChangeQueue.this; }
    }

    public final SuspendableNo performingActionProperty() { return graph.performingActionProperty(); }
    public final boolean isPerformingAction() { return graph.isPerformingAction(); }

    private final DirectedAcyclicGraph<C> graph;

    private final List<C> changes = new ArrayList<>();
    public final List<C> getChanges() { return Collections.unmodifiableList(changes); }
    private List<C> getUndoChanges() { return changes.subList(0, currentPosition); }
    private List<C> getRedoChanges() { return changes.subList(currentPosition, changes.size()); }

    private IndexedChange<C> undoNext = null;
    private IndexedChange<C> redoNext = null;

    private int currentPosition = 0;

    public UnlimitedNonLinearChangeQueue(DirectedAcyclicGraph<C> graph) {
        super();
        this.graph = graph;
        graph.registerQueue(this);
        this.mark = getCurrentPosition();
    }

    public final void close() {
        graph.unregisterQueue(this);
    }

    public final void recalculateValidChanges() {
        undoNext = null;
        List<C> undos = getUndoChanges();
        for (int i = undos.size() - 1; i >= 0; i--) {
            C possibleUndo = undos.get(i);
            if (graph.getIsValidUndo().test(possibleUndo)) {
                undoNext = new IndexedChange<>(i, possibleUndo);
                break;
            }
        }

        redoNext = null;
        List<C> redos = getRedoChanges();
        for (int i = 0; i < redos.size(); i++) {
            C possibleRedo = redos.get(i);
            if (graph.getIsValidRedo().test(possibleRedo)) {
                redoNext = new IndexedChange<>(i, possibleRedo);
                break;
            }
        }

        invalidateBindings();
    }

    @Override
    public final boolean hasNext() {
        return redoNext != null;
    }

    @Override
    public final boolean hasPrev() {
        return undoNext != null;
    }

    @Override
    public final C next() {
        C redo = redoNext.getChange();
        BubbledResult<C> bubbledResult = graph.bubbleRedo(redo);
        C bubbledChange = bubbledResult.getBubbled();

        C validRedo;
        if (redo == bubbledChange) {
            if (redoNext.getIndex() != currentPosition) {
                int index = redoNext.getIndex();
                int iterationCount = index - currentPosition;
                for (int i = 1; i <= iterationCount; i++) {
                    int oldIndex = index - i;
                    changes.set(oldIndex + 1, changes.get(oldIndex));
                }
                changes.set(currentPosition, redo);
            }
            validRedo = redo;
        } else {
            graph.updateRedosPostRedoBubble(redo, bubbledResult);
            changes.set(redoNext.getIndex(), bubbledResult.getGrounded());
            changes.add(currentPosition, bubbledChange);

            validRedo = bubbledChange;
        }

        currentPosition++;
        return validRedo;
    }

    @Override
    public final C prev() {
        C undo = undoNext.getChange();

        C validUndo;
        if (graph.isMutuallyIndependent(undo)) {
            if (undoNext.getIndex() != currentPosition - 1) {
                int changeIndex = undoNext.getIndex();
                int nextUndoIndex = currentPosition - 1;
                int iterationCount = nextUndoIndex - changeIndex;
                for (int i = 0; i < iterationCount; i++) {
                    changes.set(changeIndex + i, changes.get(changeIndex + i + 1));
                }
                changes.set(nextUndoIndex, undo);
            }
            graph.forget(Collections.singletonList(undo));

            validUndo = undo;
        } else {
            BubbledResult<C> bubbledResult = graph.bubbleUndo(undo);
            C bubbled = bubbledResult.getBubbled();
            C grounded = bubbledResult.getGrounded();

            graph.updateChangesPostUndoBubble(undo, bubbledResult);

            int changeIndex = undoNext.getIndex();
            changes.set(changeIndex, grounded);
            graph.remapEdges(undo, grounded);

            changes.add(currentPosition - 1, bubbled);

            // no need to forget bubbled change since it hasn't technically been added to graph
            //  either will be merged into new change, readded, or undone (moved to redos)
            validUndo = bubbled;
        }

        currentPosition--;
        return validUndo;
    }

    @Override
    public final void forgetHistory() {
        if(currentPosition > 0) {
            int newSize = changes.size() - currentPosition;
            for(int i = 0; i < newSize; ++i) {
                C forgottenChange = changes.get(i);
                graph.removeRelatedEdgesOf(forgottenChange);

                changes.set(i, changes.get(currentPosition + i));
            }
            changes.subList(newSize, changes.size()).clear();

            undoNext = null;
            undoAvailable.invalidate();

            currentPosition = 0;
        }
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        List<C> redos = getRedoChanges();
        graph.forget(redos);
        redos.clear();

        performingActionProperty().suspendWhile(() -> {
            for(C c: changes) {
                graph.updateChangesWithPush(c);

                this.changes.add(c);
            }
            currentPosition += changes.length;
            graph.setLatestChangeSource(this);
        });
    }

    public final void appliedRedo(C redo) {
        graph.updateChangesWithRedo(this, redo);
        graph.setLatestChangeSource(this);
    }

    public final boolean committedLastChange() { return this == graph.getLatestChangeSource(); }

    public final void updateChangesWithPush(C pushedChange) {
        getUndoChanges().replaceAll(outdatedUndo -> updateUndoWithPushedChange(outdatedUndo, pushedChange));

        updateRedosWithAddedChange(pushedChange);
    }

    public final void updateChangesWithRedo(C redoneChange) {
        if (currentPosition - 2 >= 0) {
            changes.subList(0, currentPosition - 2).replaceAll(outdatedUndo -> updateUndoWithPushedChange(outdatedUndo, redoneChange));
        }

        updateRedosWithAddedChange(redoneChange);
    }

    private C updateUndoWithPushedChange(C outdatedUndo, C pushedChange) {
        C updatedUndo = graph.getUndoUpdater().apply(pushedChange, outdatedUndo);

        graph.testForDependency(pushedChange, updatedUndo);

        if (outdatedUndo == updatedUndo) {
            return outdatedUndo;
        } else {
            graph.remapEdges(outdatedUndo, updatedUndo);
            return updatedUndo;
        }
    }

    private void updateRedosWithAddedChange(C addedChange) {
        updateRedos(outdatedRedo -> graph.getRedoUpdater().apply(addedChange, outdatedRedo));
    }

    public final void updateChangesPostUndoBubble(C original, BubbledResult<C> bubbledResult) {
        getUndoChanges().replaceAll(outdatedChange -> {
            if (outdatedChange == original) {
                return outdatedChange;
            }

            C updatedChange = graph.getUndoUpdaterPostBubble().apply(outdatedChange, original, bubbledResult);

            if (outdatedChange != updatedChange) {
                graph.remapEdges(outdatedChange, updatedChange);
                return updatedChange;
            } else {
                return outdatedChange;
            }
        });
        updateRedosPostChangeBubble(original, bubbledResult);
    }

    public final void updateRedosPostChangeBubble(C original, BubbledResult<C> bubbledResult) {
        updateRedos(outdated -> graph.getRedoUpdaterPostBubble().apply(outdated, original, bubbledResult));

    }

    private void updateRedos(Function<C, C> updater) {
        getRedoChanges().replaceAll(outdatedChange -> {
            C updatedChange = updater.apply(outdatedChange);

            return outdatedChange != updatedChange
                    ? updatedChange
                    : outdatedChange;
        });
    }

    @Override
    public QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(changes.get(currentPosition));
    }

}
