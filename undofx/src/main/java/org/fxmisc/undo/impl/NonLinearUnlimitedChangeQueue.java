package org.fxmisc.undo.impl;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;

import javax.sound.midi.Soundbank;

public class NonLinearUnlimitedChangeQueue<C> implements NonLinearChangeQueue<C> {

    private class QueuePositionImpl implements ChangeQueue.QueuePosition {
        private final NonLinearChange<C> change;

        QueuePositionImpl(NonLinearChange<C> change) {
            this.change = change;
        }

        @Override
        public boolean isValid() {
            // is valid when the given change is found in the queue's undos or redos list
            return graph.isQueuePositionValid(NonLinearUnlimitedChangeQueue.this, change);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NonLinearUnlimitedChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) obj;
                return getQueue() == otherPos.getQueue() && change == otherPos.change;
            } else {
                return false;
            }
        }

        private NonLinearUnlimitedChangeQueue<C> getQueue() { return NonLinearUnlimitedChangeQueue.this; }
    }

    public final boolean committedLastChange() { return graph.wasLastChangePerformedBy(this); }

    private final BooleanBinding undoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return hasPrev();
        }
    };
    public final ObservableBooleanValue undoAvailableProperty() { return undoAvailable; }
    public boolean isUndoAvailable() {
        return undoAvailable.get();
    }

    private final BooleanBinding redoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return hasNext();
        }
    };
    public final ObservableBooleanValue redoAvailableProperty() { return redoAvailable; }
    public boolean isRedoAvailable() {
        return redoAvailable.get();
    }

    private final BooleanBinding atMarkedPosition = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return mark.equals(getCurrentPosition());
        }
    };
    public final ObservableBooleanValue atMarkedPositionProperty() { return atMarkedPosition; }
    public boolean isAtMarkedPosition() {
        return atMarkedPosition.get();
    }

    public SuspendableNo performingActionProperty() { return graph.performingActionProperty(); }
    public boolean isPerformingAction() { return graph.isPerformingAction(); }

    private final DirectAcyclicGraphImpl<C> graph;

    private final Subscription subscription;

    private ChangeQueue.QueuePosition mark;

    public NonLinearUnlimitedChangeQueue(DirectAcyclicGraphImpl<C> graph) {
        this.graph = graph;

        this.mark = getCurrentPosition();
        this.subscription = graph.performingActionProperty().values()
                .filter(stillPerformingAction -> !stillPerformingAction)
                .subscribe(ignore -> this.invalidateProperties());
    }

    private void invalidateProperties() {
        undoAvailable.invalidate();
        redoAvailable.invalidate();
        atMarkedPosition.invalidate();
    }

    @Override
    public boolean hasNext() {
        return graph.hasNextFor(this);
    }

    @Override
    public boolean hasPrev() {
        return graph.hasPrevFor(this);
    }

    @Override
    public C next() {
        return graph.nextFor(this);
    }

    @Override
    public C prev() {
        return graph.prevFor(this);
    }

    public void addRedoableChange(C change) {
        graph.addRedoFor(this, change);
    }

    @SafeVarargs
    public final void pushChanges(C... changes) {
        graph.pushChanges(this, changes);
    }

    public final void pushRedo(C change) {
        graph.pushRedo(this, change);
    }

    @Override
    public ChangeQueue.QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(graph.getLastChangeFor(this));
    }

    public void mark() {
        mark = getCurrentPosition();
        atMarkedPosition.invalidate();
    }

    @Override
    public void forgetHistory() {
        graph.forgetHistoryFor(this, (ignore) -> undoAvailable.invalidate());
    }

    public final void close() {
        subscription.unsubscribe();
        graph.closeDown(this);
    }

}
