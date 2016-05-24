package org.fxmisc.undo;

import org.fxmisc.undo.impl.NonLinearChangeQueue;
import org.reactfx.EventStream;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface NonLinearUndoManagerFactory<S extends NonLinearChangeQueue<C>, C> {

    DirectedAcyclicUndoGraph<S, C> getGraph();

    <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply);

    <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

}
