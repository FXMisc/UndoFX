package org.fxmisc.undo;

import org.fxmisc.undo.impl.NonLinearChange;
import org.fxmisc.undo.impl.NonLinearChangeQueue;
import org.reactfx.SuspendableNo;

public interface DirectedAcyclicUndoGraph<S extends NonLinearChangeQueue<C>, C> {

    SuspendableNo performingActionProperty();

    default boolean isPerformingAction() {
        return performingActionProperty().get();
    }
}
