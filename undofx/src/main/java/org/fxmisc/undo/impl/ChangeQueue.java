package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;
import org.reactfx.SuspendableNo;

public interface ChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    public boolean isPerformingAction();

    public SuspendableNo performingActionProperty();
    
    public boolean isUndoAvailable();
    
    public ObservableBooleanValue undoAvailableProperty();

    public boolean isRedoAvailable();

    public ObservableBooleanValue redoAvailableProperty();

    public void mark(QueuePosition queuePos);

    public boolean isAtMarkedPosition();

    public ObservableBooleanValue atMarkedPositionProperty();

    public boolean hasNext();

    public boolean hasPrev();

    public C next();

    public C prev();

    @SuppressWarnings({"unchecked"})
    public void push(C... changes);

    public QueuePosition getCurrentPosition();

    public void forgetHistory();
}
