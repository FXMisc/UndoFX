package org.fxmisc.undo;

import org.fxmisc.undo.impl.nonlinear.DirectedAcyclicGraphImpl;
import org.fxmisc.undo.impl.nonlinear.NonLinearUndoManagerImpl;
import org.fxmisc.undo.impl.nonlinear.UnlimitedNonLinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.ZeroSizeNonLinearChangeQueue;
import org.reactfx.EventStream;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface NonLinearUndoManagerFactory<C> {

    DirectedAcyclicGraphImpl<C> getGraph();

    default UndoManager createUnlimited(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return createUnlimited(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    default UndoManager createUnlimited(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        UnlimitedNonLinearChangeQueue<C> queue = new UnlimitedNonLinearChangeQueue<>(getGraph());
        return new NonLinearUndoManagerImpl<>(queue, invert, apply, merge, changeStream);
    }

    default UndoManager createZero(EventStream<C> changeStream) {
        ZeroSizeNonLinearChangeQueue<C> queue = new ZeroSizeNonLinearChangeQueue<>();
        return new NonLinearUndoManagerImpl<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), changeStream);
    }

}
