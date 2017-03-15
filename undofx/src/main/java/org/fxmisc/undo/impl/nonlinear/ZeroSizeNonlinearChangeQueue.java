package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueueBase;
import org.reactfx.SuspendableNo;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class ZeroSizeNonlinearChangeQueue<C, T> extends ChangeQueueBase<C> implements NonlinearChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final long rev;

        QueuePositionImpl(long seq) {
            this.rev = seq;
        }

        @Override
        public boolean isValid() {
            return rev == ZeroSizeNonlinearChangeQueue.this.revision;
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof ZeroSizeNonlinearChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                ZeroSizeNonlinearChangeQueue.QueuePositionImpl otherPos = (ZeroSizeNonlinearChangeQueue.QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && rev == otherPos.rev;
            } else {
                return false;
            }
        }

        private ZeroSizeNonlinearChangeQueue<C, T> getQueue() {
            return ZeroSizeNonlinearChangeQueue.this;
        }
    }

    public final SuspendableNo performingActionProperty() { return graph.performingActionProperty(); }
    public final boolean isPerformingAction() { return graph.isPerformingAction(); }

    private final DirectedAcyclicGraph<C, T> graph;
    private long revision = 0;

    public ZeroSizeNonlinearChangeQueue(DirectedAcyclicGraph<C, T> graph) {
        this.graph = graph;
        this.mark = getCurrentPosition();
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrev() {
        return false;
    }

    @Override
    public C next() {
        throw new NoSuchElementException();
    }

    @Override
    public C prev() {
        throw new NoSuchElementException();
    }

    @Override
    public List<C> getChanges() {
        return Collections.emptyList();
    }

    @Override
    public List<C> getUndoChanges() { return Collections.emptyList(); }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public void recalculateValidChanges() {
        // nothing to do
    }

    @Override
    public void push(C change) {
        // no redos to forget

        performingActionProperty().suspendWhile(() -> {
            graph.updateChangesWithPush(change);
        });
        ++revision;
        atMarkedPosition.invalidate();
    }

    @Override
    public void push(C undo, C newChange) {
        throw new AssertionError("Unreachable code: A ZeroSizedNonlinearChangeQueue should never be" +
                "able to push an unmerged undo since it never returns an undo for merging in the first place.");
    }

    @Override
    public void updateChangesWithPush(C pushedChange) {
        // nothing to do
    }

    @Override
    public void updateGraphWithUndo(C undo) {
        // nothing to do
    }

    @Override
    public void updateGraphWithRedo(C redo) {
        // nothing to do
    }

    @Override
    public void updateChangesWithUndo(C undo) {
        // nothing to do
    }

    @Override
    public void updateChangesWithRedo(C redo) {
        // nothing to do
    }

    @Override
    public QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(revision);
    }

    @Override
    public void forgetHistory() {
        // nothing to forget
    }

    @Override
    public String toString() {
        return "ZeroSizeNonlinearChangeQueue(revision=" + revision + ")";
    }
}