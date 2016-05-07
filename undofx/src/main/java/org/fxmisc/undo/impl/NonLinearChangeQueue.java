package org.fxmisc.undo.impl;

import javafx.beans.value.ObservableBooleanValue;

public interface NonLinearChangeQueue<C> extends ChangeQueue<C> {

    boolean committedLastChange();

    boolean isPerformingAction();

    ObservableBooleanValue performingActionProperty();

}
