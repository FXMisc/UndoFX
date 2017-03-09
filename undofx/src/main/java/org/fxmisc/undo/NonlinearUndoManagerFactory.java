package org.fxmisc.undo;

import org.fxmisc.undo.impl.nonlinear.DirectedAcyclicGraph;
import org.fxmisc.undo.impl.nonlinear.FixedSizeNonlinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.FixedSizeNonlinearChangeQueue.BubbleStrategy;
import org.fxmisc.undo.impl.nonlinear.NonlinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.NonlinearUndoManager;
import org.fxmisc.undo.impl.nonlinear.UnlimitedNonlinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.ZeroSizeNonlinearChangeQueue;
import org.reactfx.EventStream;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface NonlinearUndoManagerFactory<C, T> {

    public BubbleStrategy getUndoBubbleStrategy();
    public void setUndoBubbleStrategy(BubbleStrategy strategy);

    public BubbleStrategy getRedoBubbleStrategy();
    public void setRedoBubbleStrategy(BubbleStrategy strategy);

    default UndoManager create(int capacity,
                               EventStream<C> changeStream,
                               Function<? super C, ? extends C> invert,
                               Consumer<C> apply) {
        return create(capacity, changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    default UndoManager create(int capacity,
                              EventStream<C> changeStream,
                              Function<? super C, ? extends C> invert,
                              Consumer<C> apply,
                              BiFunction<C, C, Optional<C>> merge) {
        if (capacity < 0) {
            return createUnlimited(changeStream, invert, apply, merge);
        } else if (capacity == 0) {
            return createFixed(capacity, changeStream, invert, apply, merge);
        } else {
            return createZero(changeStream);
        }
    }

    UndoManager createUnlimited(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

    UndoManager createFixed(
            int capacity,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

    public UndoManager createZero(EventStream<C> changeStream);

    public static <C, T> NonlinearUndoManagerFactory<C, T> factory(DirectedAcyclicGraph<C, T> graph) {
        return factory(graph, BubbleStrategy.FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE,
                BubbleStrategy.FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE);
    }

    public static <C, T> NonlinearUndoManagerFactory<C, T> factory(DirectedAcyclicGraph<C, T> graph,
                BubbleStrategy undo, BubbleStrategy redo) {
        return new NonlinearUndoManagerFactory<C, T>() {

            private BubbleStrategy undoStrategy = undo;
            @Override
            public BubbleStrategy getUndoBubbleStrategy() {
                return undoStrategy;
            }

            @Override
            public void setUndoBubbleStrategy(BubbleStrategy strategy) {
                undoStrategy = strategy;
            }

            private BubbleStrategy redoStrategy = redo;
            @Override
            public BubbleStrategy getRedoBubbleStrategy() {
                return redoStrategy;
            }

            @Override
            public void setRedoBubbleStrategy(BubbleStrategy strategy) {
                redoStrategy = strategy;
            }

            @Override
            public UndoManager createUnlimited(EventStream<C> changeStream, Function<? super C, ? extends C> invert, Consumer<C> apply, BiFunction<C, C, Optional<C>> merge) {
                NonlinearChangeQueue<C> queue = new UnlimitedNonlinearChangeQueue<>(graph);
                graph.addQueue(queue);
                return new NonlinearUndoManager<>(queue, invert, apply, merge, changeStream);
            }

            @Override
            public UndoManager createFixed(int capacity, EventStream<C> changeStream, Function<? super C, ? extends C> invert, Consumer<C> apply, BiFunction<C, C, Optional<C>> merge) {
                NonlinearChangeQueue<C> queue = new FixedSizeNonlinearChangeQueue<>(capacity, graph, undoStrategy, redoStrategy);
                graph.addQueue(queue);
                return new NonlinearUndoManager<>(queue, invert, apply, merge, changeStream);
            }

            @Override
            public UndoManager createZero(EventStream<C> changeStream) {
                NonlinearChangeQueue<C> queue = new ZeroSizeNonlinearChangeQueue<>(graph);
                // no need to register graph
                return new NonlinearUndoManager<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), changeStream);
            }
        };
    }

}
