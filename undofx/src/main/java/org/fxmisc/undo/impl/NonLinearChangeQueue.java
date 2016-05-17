package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;

public interface NonLinearChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    boolean hasNext();

    boolean hasPrev();

    C next();

    C prev();

    void addRedoableChange(C change);

    @SuppressWarnings({"unchecked"})
    void pushChanges(C... changes);

    void pushRedo(C change);

    NonLinearChangeQueue.QueuePosition getCurrentPosition();

    void forgetHistory();

    boolean committedLastChange();

    boolean isPerformingAction();

    ObservableBooleanValue performingActionProperty();

}
