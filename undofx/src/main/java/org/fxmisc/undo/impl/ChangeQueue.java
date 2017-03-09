package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;
import org.reactfx.SuspendableNo;

public interface ChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    boolean isPerformingAction();

    SuspendableNo performingActionProperty();

    boolean isUndoAvailable();

    ObservableBooleanValue undoAvailableProperty();

    boolean isRedoAvailable();

    ObservableBooleanValue redoAvailableProperty();

    void mark(QueuePosition queuePos);

    boolean isAtMarkedPosition();

    ObservableBooleanValue atMarkedPositionProperty();

    boolean hasNext();

    boolean hasPrev();

    C next();

    C prev();

    QueuePosition getCurrentPosition();

    void forgetHistory();
}
