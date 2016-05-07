package org.fxmisc.undo;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.NonLinearChange;
import org.reactfx.SuspendableNo;

import java.util.function.Supplier;

public interface DirectedAcyclicUndoGraph<S extends ChangeQueue<C>, C> {

    NonLinearChange<S, C> getValidChangeFor(NonLinearChange<S, C> nonLinearChange, Supplier<Long> bubbledRevision);

    SuspendableNo performingActionProperty();

    default boolean isPerformingAction() {
        return performingActionProperty().get();
    }
}
