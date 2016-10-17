package org.fxmisc.undo.impl.nonlinear;

import javafx.beans.value.ObservableBooleanValue;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.impl.ChangeQueue;
import org.reactfx.EventStream;
import org.reactfx.Subscription;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class NonlinearUndoManager<C> implements UndoManager {

    private class UndoPositionImpl implements UndoPosition {
        private final ChangeQueue.QueuePosition queuePos;

        UndoPositionImpl(ChangeQueue.QueuePosition queuePos) {
            this.queuePos = queuePos;
        }

        @Override
        public void mark() {
            canMerge = false;
            queue.mark(queuePos);
        }

        @Override
        public boolean isValid() {
            return queuePos.isValid();
        }
    }

    private final NonlinearChangeQueue<C> queue;
    private final Function<? super C, ? extends C> invert;
    private final Consumer<C> apply;
    private final BiFunction<C, C, Optional<C>> merge;
    private final Subscription subscription;

    private boolean canMerge;
    private C expectedChange = null;

    public NonlinearUndoManager(
            NonlinearChangeQueue<C> queue,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource) {
        this.queue = queue;
        this.invert = invert;
        this.apply = apply;
        this.merge = merge;
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
            performChange(invert.apply(queue.prev()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean redo() {
        if(isRedoAvailable()) {
            canMerge = false;
            performChange(queue.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void preventMerge() {
        canMerge = false;
    }

    @Override
    public void forgetHistory() {
        queue.forgetHistory();
    }

    private void performChange(C change) {
        this.expectedChange = change;
        queue.performingActionProperty().suspendWhile(() -> apply.accept(change));
    }

    private void changeObserved(C change) {
        if(expectedChange == null) {
            addChange(change);
        } else if(expectedChange.equals(change)) {
            expectedChange = null;
        } else {
            throw new IllegalArgumentException("Unexpected change received."
                    + "\nExpected:\n" + expectedChange
                    + "\nReceived:\n" + change);
        }
    }

    @SuppressWarnings("unchecked")
    private void addChange(C change) {
        if(canMerge && queue.hasPrev() && queue.committedLastChange()) {
            C prev = queue.prev();
            queue.push(merge(prev, change));
        } else {
            queue.push(change);
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
        return new NonlinearUndoManager.UndoPositionImpl(queue.getCurrentPosition());
    }
}