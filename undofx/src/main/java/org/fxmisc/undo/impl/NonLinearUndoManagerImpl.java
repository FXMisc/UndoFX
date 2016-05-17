package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;
import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;
import org.reactfx.Subscription;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class NonLinearUndoManagerImpl<S extends NonLinearChangeQueue<C>, C> implements UndoManager {

    // TODO: need to implement this correctly (copied from LinearUndoManagerImpl)
    private class UndoPositionImpl implements UndoPosition {
        private final NonLinearChangeQueue.QueuePosition queuePos;

        UndoPositionImpl(NonLinearChangeQueue.QueuePosition queuePos) {
            this.queuePos = queuePos;
        }

        @Override
        public void mark() {
            mark = queuePos;
            canMerge = false;
            // TODO: this should be moved to / handled somehow in ChangeQueue
            // atMarkedPositionProperty().invalidate();
        }

        @Override
        public boolean isValid() {
            return queuePos.isValid();
        }
    }

    private final Function<? super C, ? extends C> invert;
    private final Consumer<C> apply;
    private final BiFunction<C, C, Optional<C>> merge;
    private final Subscription subscription;

    private NonLinearUnlimitedChangeQueue<C> queue;
    private boolean canMerge;
    private NonLinearChangeQueue.QueuePosition mark;
    private C expectedChange = null;

    public NonLinearUndoManagerImpl(
            NonLinearUnlimitedChangeQueue<C> queue,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource) {
        this.queue = queue;
        this.invert = invert;
        this.apply = apply;
        this.merge = merge;
        this.mark = queue.getCurrentPosition();
        this.subscription = changeSource.subscribe(this::changeObserved);
    }

    @Override
    public void close() {
        queue.close();
        subscription.unsubscribe();
    }

    @Override
    public boolean undo() {
        if(isUndoAvailable()) {
            canMerge = false;
            performUndo(queue.prev());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean redo() {
        if(isRedoAvailable()) {
            canMerge = false;
            performRedo(queue.next());
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean isUndoAvailable() {
        return queue.isUndoAvailable();
    }

    @Override
    public ObservableBooleanValue undoAvailableProperty() {
        return queue.undoAvailableProperty();
    }

    @Override
    public boolean isRedoAvailable() {
        return queue.isRedoAvailable();
    }

    @Override
    public ObservableBooleanValue redoAvailableProperty() {
        return queue.redoAvailableProperty();
    }

    @Override
    public boolean isPerformingAction() {
        return queue.isPerformingAction();
    }

    @Override
    public ObservableBooleanValue performingActionProperty() {
        return queue.performingActionProperty();
    }

    @Override
    public boolean isAtMarkedPosition() {
        return queue.isAtMarkedPosition();
    }

    @Override
    public ObservableBooleanValue atMarkedPositionProperty() {
        return queue.atMarkedPositionProperty();
    }

    @Override
    public UndoPosition getCurrentPosition() {
        return new NonLinearUndoManagerImpl.UndoPositionImpl(queue.getCurrentPosition());
    }

    @Override
    public void preventMerge() {
        canMerge = false;
    }

    @Override
    public void forgetHistory() {
        queue.forgetHistory();
    }

    private void performUndo(C change) {
        performChange(invert.apply(change));
        queue.addRedoableChange(change);
    }

    private void performRedo(C change) {
        performChange(change);
        queue.pushRedo(change);
    }

    private void performChange(C change) {
        this.expectedChange = change;
        queue.performingActionProperty().suspendWhile(() -> apply.accept(change));
    }

    private void changeObserved(C change) {
        if(expectedChange == null) {
            addNewChange(change);
        } else if(expectedChange.equals(change)) {
            expectedChange = null;
        } else {
            throw new IllegalArgumentException("Unexpected change received."
                    + "\nExpected:\n" + expectedChange
                    + "\nReceived:\n" + change);
        }
    }

    @SuppressWarnings("unchecked")
    private void addNewChange(C change) {
        if(canMerge && queue.hasPrev() && queue.committedLastChange()) {
            C prev = queue.prev();
            queue.pushChanges(merge(prev, change));
        } else {
            queue.pushChanges(change);
        }
        canMerge = true;
    }

    @SuppressWarnings("unchecked")
    private C[] merge(C c1, C c2) {
        Optional<C> merged = merge.apply(c1, c2);
        if(merged.isPresent()) {
            return (C[]) new Object[] { merged.get() };
        } else {
            return (C[]) new Object[] { c1, c2 };
        }
    }

}
