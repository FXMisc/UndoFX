package org.fxmisc.undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.ConsistentNonLinearUndoManager;
import org.fxmisc.undo.impl.FixedSizeChangeQueue;
import org.fxmisc.undo.impl.LinearUndoManager;
import org.fxmisc.undo.impl.NonLinearUndoManager;
import org.fxmisc.undo.impl.UnlimitedChangeQueue;
import org.reactfx.EventStream;

/**
 * Created by jordan on 3/12/16.
 */
public interface NonLinearUndoManagerFactory {

    <S, C> UndoManager<S> createNonLinear(
            S source,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

    <S, C> UndoManager<S> createNonLinear(
            S source,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply);

    public static <S, C> UndoManager<S> unlimitedHistoryNonLinearManager(
            S source,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        BiFunction<C, C, Optional<C>> merge = (c1, c2) -> Optional.empty();
        LinearUndoManager<C> first = new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
        return new NonLinearUndoManager<>(source, first);
    }

    public static <S, C> UndoManager<S> unlimitedHistoryNonLinearManager(
            S source,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        LinearUndoManager<C> first = new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
        return new NonLinearUndoManager<>(source, first);
    }

    public static NonLinearUndoManagerFactory unlimitedHistoryFactory() {
        return new NonLinearUndoManagerFactory() {
            @Override
            public <S, C> UndoManager<S> createNonLinear(
                    S source,
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply) {
                return unlimitedHistoryNonLinearManager(source, changeStream, invert, apply);
            }

            @Override
            public <S, C> UndoManager<S> createNonLinear(
                    S source,
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge) {
                return unlimitedHistoryNonLinearManager(source, changeStream, invert, apply, merge);
            }
        };
    }

    public static <S, C> UndoManager<S> fixedSizeHistoryNonLinearManager(
            S source,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            int capacity) {
        ChangeQueue<C> queue = new FixedSizeChangeQueue<C>(capacity);
        BiFunction<C, C, Optional<C>> merge = (c1, c2) -> Optional.empty();
        LinearUndoManager<C> first = new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
        return new NonLinearUndoManager<>(source, first);
    }

    public static <S, C> UndoManager<S> fixedSizeHistoryNonLinearManager(
            S source,
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        ChangeQueue<C> queue = new FixedSizeChangeQueue<C>(capacity);
        LinearUndoManager<C> first = new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
        return new NonLinearUndoManager<>(source, first);
    }

    public static NonLinearUndoManagerFactory fixedSizeHistoryFactory(int capacity) {
        return new NonLinearUndoManagerFactory() {
            @Override
            public <S, C> UndoManager<S> createNonLinear(
                    S source,
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply) {
                return fixedSizeHistoryNonLinearManager(source, changeStream, invert, apply, capacity);
            }

            @Override
            public <S, C> UndoManager<S> createNonLinear(
                    S source,
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge) {
                return fixedSizeHistoryNonLinearManager(source, changeStream, invert, apply, merge, capacity);
            }
        };
    }

    public static <S, C> UndoManager<S> consistentUndoManager(
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource,
            S initialSource,
            ChangeQueue<C> queue) {
        return new ConsistentNonLinearUndoManager<>(invert, apply, merge, changeSource, initialSource, queue);
    }

}
