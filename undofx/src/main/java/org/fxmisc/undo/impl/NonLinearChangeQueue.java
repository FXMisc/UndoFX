package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;

// TODO move from impl package to undo package
public interface NonLinearChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    boolean hasNext();

    boolean hasPrev();

    C next();

    C prev();

    @SuppressWarnings({"unchecked"})
    void pushChanges(C... changes);

    void pushRedo(C change);

    NonLinearChangeQueue.QueuePosition getCurrentPosition();

    void forgetHistory();

    boolean committedLastChange();

    boolean isPerformingAction();

    ObservableBooleanValue performingActionProperty();

    // TODO: Remove this requirement as not needed when using Maps
    int getId();

    void addRedoableChange(C change);

}
