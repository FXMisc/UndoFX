package org.fxmisc.undo.impl;

import javafx.beans.binding.BooleanBinding;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;

public class NonLinearUnlimitedChangeQueue<C> implements NonLinearChangeQueue<C> {

    // TODO: This still needs to be properly implemented (copied from UnlimitedLinearChangeQueue
    private class NonLinearQueuePositionImpl implements ChangeQueue.QueuePosition {
        private final int allTimePos;
        private final long rev;

        NonLinearQueuePositionImpl(int allTimePos, long rev) {
            this.allTimePos = allTimePos;
            this.rev = rev;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    // TODO: Test this
    // A replacement for UndoManager#canMerge: a merge should only be able to occur when this queue made the last change.
    // Otherwise, it might be outdated or conflict with some other change from another queue.
    public final boolean committedLastChange() { return graph.wasLastChangePerformedBy(this); }

    private final BooleanBinding undoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return hasPrev();
        }
    };
    public final BooleanBinding undoAvailableProperty() { return undoAvailable; }
    public boolean isUndoAvailable() {
        return undoAvailable.get();
    }

    private final BooleanBinding redoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return hasNext();
        }
    };
    public final BooleanBinding redoAvailableProperty() { return redoAvailable; }
    public boolean isRedoAvailable() {
        return redoAvailable.get();
    }

    private final BooleanBinding atMarkedPosition = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return mark.equals(getCurrentPosition());
        }
    };
    public final BooleanBinding atMarkedPositionProperty() { return atMarkedPosition; }
    public boolean isAtMarkedPosition() {
        return atMarkedPosition.get();
    }

    public SuspendableNo performingActionProperty() { return graph.performingActionProperty(); }
    public boolean isPerformingAction() { return graph.isPerformingAction(); }

    private final DirectAcyclicGraphImpl<NonLinearUnlimitedChangeQueue<C>, C> graph;

    private final Subscription subscription;

    private int forgottenCount = 0;
    private ChangeQueue.QueuePosition mark;


    public NonLinearUnlimitedChangeQueue(DirectAcyclicGraphImpl<NonLinearUnlimitedChangeQueue<C>, C> graph) {
        this.graph = graph;
        graph.registerRedoableListFor(this);

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
        graph.addRedoableChangeFor(this, change);
    }

    @SafeVarargs
    public final void pushChanges(C... changes) {
        graph.pushChanges(this, changes);
    }

    public final void pushRedo(C change) {
        graph.pushRedo(this, change);
    }

    // TODO: implement this correctly
    @Override
    public NonLinearChangeQueue.QueuePosition getCurrentPosition() {
        return null;
    }

    @Override
    public void forgetHistory() {
        graph.forgetHistoryFor(this, forgottenSize -> {
            forgottenCount += forgottenSize;
            undoAvailable.invalidate();
        });
    }

    public final void close() {
        subscription.unsubscribe();
        graph.closeDown(this);
    }

}
