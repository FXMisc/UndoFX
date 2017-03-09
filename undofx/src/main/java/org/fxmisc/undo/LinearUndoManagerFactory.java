package org.fxmisc.undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.fxmisc.undo.impl.linear.FixedSizeLinearChangeQueue;
import org.fxmisc.undo.impl.linear.LinearChangeQueue;
import org.fxmisc.undo.impl.linear.LinearUndoManager;
import org.fxmisc.undo.impl.linear.UnlimitedLinearChangeQueue;
import org.fxmisc.undo.impl.linear.ZeroSizeLinearChangeQueue;
import org.reactfx.EventStream;

public interface LinearUndoManagerFactory {

    <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply);

    <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge);

    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        LinearChangeQueue<C> queue = new UnlimitedLinearChangeQueue<C>();
        BiFunction<C, C, Optional<C>> merge = (c1, c2) -> Optional.empty();
        return new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
    }

    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        LinearChangeQueue<C> queue = new UnlimitedLinearChangeQueue<C>();
        return new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
    }

    public static LinearUndoManagerFactory unlimitedHistoryFactory() {
        return new LinearUndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply) {
                return unlimitedHistoryUndoManager(changeStream, invert, apply);
            }

            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge) {
                return unlimitedHistoryUndoManager(changeStream, invert, apply, merge);
            }
        };
    }

    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            int capacity) {
        LinearChangeQueue<C> queue = new FixedSizeLinearChangeQueue<C>(capacity);
        BiFunction<C, C, Optional<C>> merge = (c1, c2) -> Optional.empty();
        return new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
    }

    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        LinearChangeQueue<C> queue = new FixedSizeLinearChangeQueue<C>(capacity);
        return new LinearUndoManager<>(queue, invert, apply, merge, changeStream);
    }

    public static LinearUndoManagerFactory fixedSizeHistoryFactory(int capacity) {
        return new LinearUndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply) {
                return fixedSizeHistoryUndoManager(changeStream, invert, apply, capacity);
            }

            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge) {
                return fixedSizeHistoryUndoManager(changeStream, invert, apply, merge, capacity);
            }
        };
    }

    public static <C> UndoManager zeroHistoryUndoManager(EventStream<C> changeStream) {
        LinearChangeQueue<C> queue = new ZeroSizeLinearChangeQueue<>();
        return new LinearUndoManager<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), changeStream);
    }

    public static LinearUndoManagerFactory zeroHistoryFactory() {
        return new LinearUndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply) {
                return zeroHistoryUndoManager(changeStream);
            }

            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge) {
                return zeroHistoryUndoManager(changeStream);
            }
        };
    }
}