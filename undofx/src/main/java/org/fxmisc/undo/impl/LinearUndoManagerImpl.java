package org.fxmisc.undo.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;

import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;

public class LinearUndoManagerImpl<C> implements UndoManager {

    private class UndoPositionImpl implements UndoPosition {
        private final ChangeQueue.QueuePosition queuePos;

        UndoPositionImpl(ChangeQueue.QueuePosition queuePos) {
            this.queuePos = queuePos;
        }

        @Override
        public void mark() {
            mark = queuePos;
            canMerge = false;
            atMarkedPosition.invalidate();
        }

        @Override
        public boolean isValid() {
            return queuePos.isValid();
        }
    }

    private final LinearChangeQueue<C> queue;
    private final Function<? super C, ? extends C> invert;
    private final Consumer<C> apply;
    private final BiFunction<C, C, Optional<C>> merge;
    private final Subscription subscription;
    private final SuspendableNo performingAction = new SuspendableNo();

    private final BooleanBinding undoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return queue.hasPrev();
        }
    };

    private final BooleanBinding redoAvailable = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return queue.hasNext();
        }
    };

    private final BooleanBinding atMarkedPosition = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return mark.equals(queue.getCurrentPosition());
        }
    };

    private boolean canMerge;
    private ChangeQueue.QueuePosition mark;
    private C expectedChange = null;

    public LinearUndoManagerImpl(
            LinearChangeQueue<C> queue,
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
        subscription.unsubscribe();
    }

    @Override
    public boolean undo() {
        if(isUndoAvailable()) {
            canMerge = false;
            performChange(invert.apply(queue.prev()));
            undoAvailable.invalidate();
            redoAvailable.invalidate();
            atMarkedPosition.invalidate();
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
            undoAvailable.invalidate();
            redoAvailable.invalidate();
            atMarkedPosition.invalidate();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isUndoAvailable() {
        return undoAvailable.get();
    }

    @Override
    public ObservableBooleanValue undoAvailableProperty() {
        return undoAvailable;
    }

    @Override
    public boolean isRedoAvailable() {
        return redoAvailable.get();
    }

    @Override
    public ObservableBooleanValue redoAvailableProperty() {
        return redoAvailable;
    }

    @Override
    public boolean isPerformingAction() {
        return performingAction.get();
    }

    @Override
    public ObservableBooleanValue performingActionProperty() {
        return performingAction;
    }

    @Override
    public boolean isAtMarkedPosition() {
        return atMarkedPosition.get();
    }

    @Override
    public ObservableBooleanValue atMarkedPositionProperty() {
        return atMarkedPosition;
    }

    @Override
    public UndoPosition getCurrentPosition() {
        return new UndoPositionImpl(queue.getCurrentPosition());
    }

    @Override
    public void preventMerge() {
        canMerge = false;
    }

    @Override
    public void forgetHistory() {
        queue.forgetHistory();
        undoAvailable.invalidate();
    }

    private void performChange(C change) {
        this.expectedChange = change;
        performingAction.suspendWhile(() -> apply.accept(change));
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
        if(canMerge && queue.hasPrev()) {
            C prev = queue.prev();
            queue.push(merge(prev, change));
        } else {
            queue.push(change);
        }
        canMerge = true;
        undoAvailable.invalidate();
        redoAvailable.invalidate();
        atMarkedPosition.invalidate();
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
