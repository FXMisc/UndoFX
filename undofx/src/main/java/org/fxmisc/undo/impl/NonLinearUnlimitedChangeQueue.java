package org.fxmisc.undo.impl;

import javafx.beans.binding.BooleanBinding;
import javafx.collections.transformation.FilteredList;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;

import java.util.ArrayList;
import java.util.List;

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
    private final BooleanBinding committedLastChange = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return graph.getLastChangeSource().equals(NonLinearUnlimitedChangeQueue.this);
        }
    };
    public final BooleanBinding committedLastChangeProperty() { return committedLastChange; }
    public final boolean committedLastChange() { return committedLastChange.get(); }

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

    // TODO: Should this still be used for equals? (NonLinearUndoManagerFactory can pass a new ID each time or something, but there may be better way
    private final int id;
    public final int getId() { return id; }

    private final DirectAcyclicGraphImpl<NonLinearUnlimitedChangeQueue<C>, C> graph;
    private final FilteredList<NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C>> allChanges;
    private final FilteredList<NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C>> validChanges;

    private final Subscription subscription;

    private int forgottenCount = 0;
    private ChangeQueue.QueuePosition mark;


    public NonLinearUnlimitedChangeQueue(int id, DirectAcyclicGraphImpl<NonLinearUnlimitedChangeQueue<C>, C> graph) {
        this.id = id;
        this.graph = graph;
        graph.addRedoableListFor(this);
        allChanges = graph.allChangesFor(this);
        validChanges = graph.validChangesFor(this);

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
        return !validChanges.isEmpty();
    }

    @Override
    public C next() {
        return graph.nextFor(this);
    }

    @Override
    public C prev() {
        NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C> previous = validChanges.get(validChanges.size() - 1);
        return graph.getValidChange(previous).getChange();
    }

    public void addRedoableChange(C change) {
        graph.addRedoableChange(this, change);
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
        if(allChanges.size() > 1 && validChanges.size() > 1) {
            List<NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C>> forgottenChanges = new ArrayList<>();
            NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C> currentChange = allChanges.get(0);
            NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C> lastValidChange = validChanges.get(validChanges.size() - 1);
            int i = 1;
            while (!lastValidChange.equals(currentChange)) {
                forgottenChanges.add(currentChange);
                currentChange = allChanges.get(i++);
            }

            graph.forgetChanges(forgottenChanges);
            forgottenCount += forgottenChanges.size();
            undoAvailable.invalidate();
        }
    }

    public final void close() {
        subscription.unsubscribe();
        graph.closeDown(this);
    }

    // TODO: Figure out best way to implement equals here... Perhaps using DAG in some way?
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NonLinearUnlimitedChangeQueue) {
            NonLinearUnlimitedChangeQueue that = (NonLinearUnlimitedChangeQueue) obj;
            return this.id == that.getId();
        } else {
            return false;
        }
    }

}
