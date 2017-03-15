package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueueBase;
import org.reactfx.SuspendableNo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UnlimitedNonlinearChangeQueue<C, T> extends ChangeQueueBase<C> implements NonlinearChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final Version<C> storedChange;
        private final int currentForgottenRevision;

        QueuePositionImpl(Version<C> storedChange, int currentForgottenRevision) {
            this.storedChange = storedChange;
            this.currentForgottenRevision = currentForgottenRevision;
        }

        /**
         * True if this position was created while the queue was empty and {@link #forgetHistory()} has not yet been
         * called since this position's creation or if the queue contains the storedChange (the original or updated one,
         * but not a bubbled one), or if a change at the stored index equals the stored change that is now at that index.
         */
        @Override
        public boolean isValid() {
            if (storedChange == null) {
                return currentForgottenRevision == forgottenRevisionCount;
            } else {
                return !changes.isEmpty() && (changesContainsStoredIdentity() || equalChangeAtStoredIndex());
            }
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof UnlimitedNonlinearChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) obj;
                return getQueue() == otherPos.getQueue()
                        && otherPos.storedChange != null
                            ? storedChange != null && storedChange.identityEquals(otherPos.storedChange)
                            : currentForgottenRevision == otherPos.currentForgottenRevision;
            } else {
                return false;
            }
        }

        private boolean equalChangeAtStoredIndex() {
            if (changes.size() <= storedChange.getIndex()) {
                return false;
            }

            C changeAtStoredIndex = changes.get(storedChange.getIndex()).getChange();
            return changeAtStoredIndex.equals(storedChange.getChange()) && currentForgottenRevision == forgottenRevisionCount;
        }

        private boolean changesContainsStoredIdentity() {
            return changes.stream().map(Version::getIdentity).anyMatch(i -> i == storedChange.getIdentity());
        }

        private UnlimitedNonlinearChangeQueue<C, T> getQueue() { return UnlimitedNonlinearChangeQueue.this; }


        @Override
        public String toString() {
            return String.format("UnlimitedNonlinearChangeQueuePosition(change=%s currentForgottenRevision=%s", storedChange, currentForgottenRevision);
        }
    }

    public final SuspendableNo performingActionProperty() { return graph.performingActionProperty(); }
    public final boolean isPerformingAction() { return graph.isPerformingAction(); }

    private final DirectedAcyclicGraph<C, T> graph;

    private final List<Version<C>> changes;
    private List<C> getUnversionedChanges() { return changes.stream().map(Version::getChange).collect(Collectors.toList()); }
    public final List<C> getChanges() { return getUnversionedChanges(); }
    public final List<C> getUndoChanges() { return getUnversionedChanges().subList(0, currentPosition); }
    private List<Version<C>> getUndos() { return changes.subList(0, currentPosition); }
    private List<Version<C>> getRedos() { return changes.subList(currentPosition, changes.size()); }

    private int undoNext = -1;
    private int getUndoNext() { return undoNext; }
    private int redoNext = -1;
    private int getRedoNext() { return redoNext; }

    private int currentPosition = 0;
    private int forgottenRevisionCount = 0;

    public UnlimitedNonlinearChangeQueue(DirectedAcyclicGraph<C, T> graph) {
        super();
        changes = new ArrayList<>();
        this.graph = graph;
        this.mark = getCurrentPosition();
    }

    public final void close() {
        graph.removeQueue(this);
    }

    public final void recalculateValidChanges() {
        undoNext = -1;
        List<Version<C>> undos = getUndos();
        if (!undos.isEmpty()) {
            for (int i = undos.size() - 1; i >= 0; i--) {
                Version<C> possibleUndo = undos.get(i);
                if (graph.isUndoValid(possibleUndo.getChange())) {
                    undoNext = i;
                    break;
                }
            }
        }

        redoNext = -1;
        List<Version<C>> redos = getRedos();
        if (!redos.isEmpty()) {
            for (int i = 0; i < redos.size(); i++) {
                Version<C> possibleRedo = redos.get(i);
                if (graph.isRedoValid(possibleRedo.getChange())) {
                    redoNext = currentPosition + i;
                    break;
                }
            }
        }

        invalidateBindings();
    }

    @Override
    public final boolean hasNext() {
        return redoNext != -1;
    }

    @Override
    public final boolean hasPrev() {
        return undoNext != -1;
    }

    @Override
    public final C next() {
        int redoNext = getRedoNext();
        Version<C> redoContainer = changes.get(redoNext);
        C redo = redoContainer.getChange();
        BubbledRedoResult<C> bubbledRedoResult = graph.bubbleRedo(redo);

        C validRedo;
        if (!bubbledRedoResult.wasBubbled()) {
            // if redo is not next redo, rewrite history so it is
            if (redoNext != currentPosition) {
                moveChangesByOneIndex(RewriteDirection.AWAY_FROM_ZERO_INDEX, currentPosition, redoNext);
                changes.set(currentPosition, redoContainer);
            }
            validRedo = redo;
        } else {
            changes.set(redoNext, new Version<>(redoNext, bubbledRedoResult.getGrounded()));
            changes.add(currentPosition, new Version<>(currentPosition, bubbledRedoResult.getBubbled()));

            validRedo = bubbledRedoResult.getBubbled();
        }

        currentPosition++;
        return validRedo;
    }

    @Override
    public final C prev() {
        int undoNext = getUndoNext();
        Version<C> undoContainer = changes.get(undoNext);
        C undo = undoContainer.getChange();

        C validUndo;
        if (graph.isMutuallyIndependent(undo)) {
            // if undo is not next undo, rewrite history so it is
            if (undoNext != currentPosition - 1) {
                int nextUndoIndex = currentPosition - 1;
                moveChangesByOneIndex(RewriteDirection.TOWARDS_ZERO_INDEX, undoNext, nextUndoIndex);
                changes.set(nextUndoIndex, undoContainer);
            }
            graph.forgetUndos(Collections.singletonList(undo));

            validUndo = undo;
        } else {
            BubbledUndoResult<C, T> bubbledUndoResult = graph.bubbleUndo(undo);
            C bubbled = bubbledUndoResult.getBubbled();
            C grounded = bubbledUndoResult.getGrounded();

            changes.set(undoNext, new Version<>(undoNext, grounded));
            graph.remapBubbledUndoDependencies(undo, bubbledUndoResult);

            changes.add(currentPosition - 1, new Version<>(currentPosition - 1, bubbled));

            // no need to forget bubbled change since it hasn't technically been added to graph
            //  either will be merged into new change, readded, or undone (moved to redos)
            validUndo = bubbled;
        }

        currentPosition--;
        return validUndo;
    }

    /** Moves changes.subList(from, to) by one index in the given direction */
    private void moveChangesByOneIndex(RewriteDirection direction, int from, int to) {
        Consumer<Integer> rewriter = direction.equals(RewriteDirection.TOWARDS_ZERO_INDEX)
                ? (i) -> changes.set(from + i, changes.get(from + i + 1))
                : (i) -> changes.set(to - i, changes.get(to - i - i));

        int iterationCount = to - from;
        for (int i = 0; i < iterationCount; i++) {
            rewriter.accept(i);
        }
    }

    public final void updateGraphWithUndo(C undo) {
        graph.updateChangesWithUndo(this, undo);
    }

    public final void updateGraphWithRedo(C redo) {
        graph.updateChangesWithRedo(this, redo);
    }

    public final void updateChangesWithUndo(C undo) {
        replaceAllUndos(outdatedUndo -> updateUndoWithPushedUndo(outdatedUndo, undo));
    }

    @Override
    public final void forgetHistory() {
        if(currentPosition > 0) {
            graph.stopTrackingDependenciesOf(getUndoChanges());

            int newSize = changes.size() - currentPosition;
            for(int i = 0; i < newSize; ++i) {
                changes.set(i, changes.get(currentPosition + i));
            }
            changes.subList(newSize, changes.size()).clear();

            undoNext = -1;
            undoAvailable.invalidate();
            redoNext = redoNext - getUndos().size();

            currentPosition = 0;
            forgottenRevisionCount++;
        }
    }

    @Override
    public final void push(C change) {
        performingActionProperty().suspendWhile(() -> doPush(change));
    }

    private void doPush(C newChange) {
        getRedos().clear();

        graph.updateChangesWithPush(newChange);
        changes.add(new Version<C>(currentPosition, newChange));
        currentPosition++;
    }

    @Override
    public void push(C undo, C newChange) {
        performingActionProperty().suspendWhile(() -> {
            C expectedUndo = changes.get(currentPosition).getChange();
            if (expectedUndo != undo) {
                throw new IllegalStateException("An unmerged undo was not the undo this queue expected.");
            }

            currentPosition++;
            doPush(newChange);
        });
    }

    public final void updateChangesWithPush(C pushedChange) {
        replaceAllUndos(outdatedUndo -> updateUndoWithPushedChange(outdatedUndo, pushedChange));

        updateRedosWithAddedChange(pushedChange);
    }

    public final void updateChangesWithRedo(C redoneChange) {
        // [0,    1,    2,    3,                       4]
        // [undo, undo, undo, undo that is now a redo, currentPosition (since it was already incremented) in next()]
        // undos == changes.subList(0, currentPosition - 2)
        if (0 <= currentPosition - 2) {
            replaceChanges(changes.subList(0, currentPosition - 2), outdatedUndo -> updateUndoWithPushedChange(outdatedUndo, redoneChange));
        }

        updateRedosWithAddedChange(redoneChange);
    }

    private C updateUndoWithPushedUndo(C outdatedUndo, C pushedUndo) {
        C updatedUndo = graph.updateUndo(outdatedUndo, pushedUndo);

        // TODO: does this need to be here? Are dependencies already there? Or will a new change cause a new dependency to be added?
        graph.addDependencyIfExists(pushedUndo, outdatedUndo);

        if (!outdatedUndo.equals(updatedUndo)) {
            graph.remapEdges(outdatedUndo, updatedUndo);
        }

        return updatedUndo;
    }

    private C updateUndoWithPushedChange(C outdatedUndo, C pushedChange) {
        C updatedUndo = graph.updateUndo(outdatedUndo, pushedChange);

        graph.addDependencyIfExists(pushedChange, updatedUndo);

        if (!outdatedUndo.equals(updatedUndo)) {
            graph.remapEdges(outdatedUndo, updatedUndo);
        }

        return updatedUndo;
    }

    private void updateRedosWithAddedChange(C addedChange) {
        replaceAllRedos(outdatedRedo -> graph.updateRedo(outdatedRedo, addedChange));
    }

    private void replaceAllUndos(Function<C, C> updater) {
        replaceChanges(getUndos(), updater);
    }

    private void replaceAllRedos(Function<C, C> updater) {
        replaceChanges(getRedos(), updater);
    }

    private void replaceChanges(List<Version<C>> changeList, Function<C, C> updater) {
        changeList.replaceAll(outdatedVersion -> {
            C oldChange = outdatedVersion.getChange();
            C newChange = updater.apply(oldChange);
            return outdatedVersion.setChange(newChange);
        });
    }

    @Override
    public QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(changes.isEmpty() || currentPosition == 0 ? null : changes.get(currentPosition - 1), forgottenRevisionCount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UnlimitedNonlinearChangeQueue(currentPosition=").append(currentPosition)
                .append(" forgottenRevisionCount=").append(forgottenRevisionCount)
                .append(" undoNext=").append(undoNext)
                .append(" redoNext=").append(redoNext)
                .append(" changes=[\n");
        int index = 0;
        for (Version<C> vChange : changes) {
            sb.append("\t").append("Index #").append(index).append(": ")
                    .append(vChange.getChange().toString());
            if (undoNext == index) {
                sb.append(" <-- Next Undo\n");
            } else if (redoNext == index) {
                sb.append(" <-- Next Redo\n");
            } else {
                sb.append("\n");
            }
            index++;
        }
        sb.append("])");
        return sb.toString();
    }
}