package org.fxmisc.undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.linear.FixedSizeLinearChangeQueue;
import org.fxmisc.undo.impl.linear.LinearUndoManager;
import org.fxmisc.undo.impl.linear.UnlimitedLinearChangeQueue;
import org.fxmisc.undo.impl.linear.ZeroSizeLinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.NonlinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.NonlinearUndoManager;
import org.fxmisc.undo.impl.nonlinear.UnlimitedNonlinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.ZeroSizeNonlinearChangeQueue;
import org.reactfx.EventStream;

public interface UndoManagerFactory<C> {

    default UndoManager unlimitedHistory(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return unlimitedHistory(changeStream, invert, apply,(c1, c2) -> Optional.empty() );
    }

    UndoManager unlimitedHistory(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

    default UndoManager fixedSizeHistory(
            int capacity,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return fixedSizeHistory(capacity, changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    UndoManager fixedSizeHistory(
            int capacity,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

    UndoManager zeroHistory(EventStream<C> changeStream);

    public static <C> UndoManagerFactory<C> linearFactory() {
        return new UndoManagerFactory<C>() {
            @Override
            public UndoManager unlimitedHistory(EventStream<C> changeStream, Function<? super C, ? extends C> invert,
                                                Consumer<C> apply, BiFunction<C, C, Optional<C>> merge) {
                ChangeQueue<C> queue = new UnlimitedLinearChangeQueue<C>();
                return new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
            }

            @Override
            public UndoManager fixedSizeHistory(int capacity,
                                                EventStream<C> changeStream, Function<? super C, ? extends C> invert,
                                                Consumer<C> apply, BiFunction<C, C, Optional<C>> merge) {
                ChangeQueue<C> queue = new FixedSizeLinearChangeQueue<C>(capacity);
                return new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
            }

            @Override
            public UndoManager zeroHistory(EventStream<C> changeStream) {
                ChangeQueue<C> queue = new ZeroSizeLinearChangeQueue<>();
                return new LinearUndoManager<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), changeStream);
            }
        };
    }

    public static <C> UndoManagerFactory<C> nonlinearFactory(DirectedAcyclicGraph<C> graph) {
        return new UndoManagerFactory<C>() {
            @Override
            public UndoManager unlimitedHistory(EventStream<C> changeStream, Function<? super C, ? extends C> invert,
                                                Consumer<C> apply, BiFunction<C, C, Optional<C>> merge) {
                NonlinearChangeQueue<C> queue = new UnlimitedNonlinearChangeQueue<C>(graph);
                return new NonlinearUndoManager<>(queue, invert, apply, merge, changeStream);
            }

            @Override
            public UndoManager fixedSizeHistory(int capacity,
                                                EventStream<C> changeStream, Function<? super C, ? extends C> invert,
                                                Consumer<C> apply, BiFunction<C, C, Optional<C>> merge) {
                NonlinearChangeQueue<C> queue = new UnlimitedNonlinearChangeQueue<C>(graph);
                return new NonlinearUndoManager<>(queue, invert, apply, merge, changeStream);
            }

            @Override
            public UndoManager zeroHistory(EventStream<C> changeStream) {
                NonlinearChangeQueue<C> queue = new ZeroSizeNonlinearChangeQueue<C>();
                return new NonlinearUndoManager<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), changeStream);
            }
        };
    }

}
