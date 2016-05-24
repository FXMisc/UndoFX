package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;
import org.reactfx.SuspendableNo;

public interface NonLinearChangeQueue<C> extends ChangeQueue<C> {

    public void addRedoableChange(C change);

    @SuppressWarnings({"unchecked"})
    public void pushChanges(C... changes);

    public void pushRedo(C change);

    public boolean committedLastChange();

    public ObservableBooleanValue undoAvailableProperty();
    public boolean isUndoAvailable();

    public ObservableBooleanValue redoAvailableProperty();
    public boolean isRedoAvailable();

    public ObservableBooleanValue atMarkedPositionProperty();
    public boolean isAtMarkedPosition();

    public void mark();

    public boolean isPerformingAction();

    public SuspendableNo performingActionProperty();

    public void close();

}
