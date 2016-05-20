package org.fxmisc.undo;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.NonLinearChange;
import org.fxmisc.undo.impl.NonLinearChangeQueue;
import org.reactfx.SuspendableNo;

import java.util.function.Supplier;

public interface DirectedAcyclicUndoGraph<S extends NonLinearChangeQueue<C>, C> {

    NonLinearChange<S, C> getValidFormOf(NonLinearChange<S, C> nonLinearChange);

    SuspendableNo performingActionProperty();

    default boolean isPerformingAction() {
        return performingActionProperty().get();
    }
}