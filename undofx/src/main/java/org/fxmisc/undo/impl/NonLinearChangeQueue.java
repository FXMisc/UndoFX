package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;

public interface NonLinearChangeQueue<C> extends ChangeQueue<C> {

    void addRedoableChange(C change);

    @SuppressWarnings({"unchecked"})
    void pushChanges(C... changes);

    void pushRedo(C change);

    boolean committedLastChange();

    boolean isPerformingAction();

    ObservableBooleanValue performingActionProperty();

}
