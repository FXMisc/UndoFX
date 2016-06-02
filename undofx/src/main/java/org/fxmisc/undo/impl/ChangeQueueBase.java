package org.fxmisc.undo.impl;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import org.reactfx.SuspendableNo;

public abstract class ChangeQueueBase<C> implements ChangeQueue<C> {

    private final SuspendableNo performingAction = new SuspendableNo();
    @Override public boolean isPerformingAction() { return performingAction.get(); }
    @Override public SuspendableNo performingActionProperty() { return performingAction; }

    protected final BooleanBinding undoAvailable = new BooleanBinding() {
        @Override protected boolean computeValue() { return hasPrev(); }
    };
    @Override public boolean isUndoAvailable() { return undoAvailable.get(); }
    @Override public ObservableBooleanValue undoAvailableProperty() { return undoAvailable; }

    protected final BooleanBinding redoAvailable = new BooleanBinding() {
        @Override protected boolean computeValue() { return hasNext(); }
    };
    @Override public boolean isRedoAvailable() { return redoAvailable.get(); }
    @Override public ObservableBooleanValue redoAvailableProperty() { return redoAvailable; }

    protected QueuePosition mark;
    @Override public void mark(QueuePosition queuePos) {
        this.mark = queuePos;
        atMarkedPosition.invalidate();
    }

    protected final BooleanBinding atMarkedPosition = new BooleanBinding() {
        @Override protected boolean computeValue() { return mark.equals(getCurrentPosition()); }
    };
    @Override public boolean isAtMarkedPosition() { return atMarkedPosition.get(); }
    @Override public ObservableBooleanValue atMarkedPositionProperty() { return atMarkedPosition; }

    protected final void invalidateBindings() {
        undoAvailable.invalidate();
        redoAvailable.invalidate();
        atMarkedPosition.invalidate();
    }

}
