package org.fxmisc.undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.FixedSizeChangeQueue;
import org.fxmisc.undo.impl.UndoManagerImpl;
import org.fxmisc.undo.impl.UnlimitedChangeQueue;
import org.fxmisc.undo.impl.ZeroSizeChangeQueue;
import org.reactfx.EventStream;

public interface UndoManagerFactory {

    <C> UndoManager create(
            EventStream<C> changeStream,
            Consumer<C> apply,
            Consumer<C> undo);

    <C> UndoManager create(
            EventStream<C> changeStream,
            Consumer<C> apply,
            Consumer<C> undo,
            BiFunction<C, C, Optional<C>> merge);

    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Consumer<C> apply,
            Consumer<C> undo) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        BiFunction<C, C, Optional<C>> merge = (c1, c2) -> Optional.empty();
        return new UndoManagerImpl<>(queue, apply, undo, merge, changeStream);
    }

    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Consumer<C> apply,
            Consumer<C> undo,
            BiFunction<C, C, Optional<C>> merge) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        return new UndoManagerImpl<>(queue, apply, undo, merge, changeStream);
    }

    public static UndoManagerFactory unlimitedHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Consumer<C> apply,
                    Consumer<C> undo) {
                return unlimitedHistoryUndoManager(changeStream, apply, undo);
            }

            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Consumer<C> apply,
                    Consumer<C> undo,
                    BiFunction<C, C, Optional<C>> merge) {
                return unlimitedHistoryUndoManager(changeStream, apply, undo, merge);
            }
        };
    }

    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Consumer<C> apply,
            Consumer<C> undo,
            int capacity) {
        ChangeQueue<C> queue = new FixedSizeChangeQueue<C>(capacity);
        BiFunction<C, C, Optional<C>> merge = (c1, c2) -> Optional.empty();
        return new UndoManagerImpl<>(queue, apply, undo, merge, changeStream);
    }

    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Consumer<C> apply,
            Consumer<C> undo,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        ChangeQueue<C> queue = new FixedSizeChangeQueue<C>(capacity);
        return new UndoManagerImpl<>(queue, apply, undo, merge, changeStream);
    }

    public static UndoManagerFactory fixedSizeHistoryFactory(int capacity) {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Consumer<C> apply,
                    Consumer<C> undo) {
                return fixedSizeHistoryUndoManager(changeStream, apply, undo, capacity);
            }

            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Consumer<C> apply,
                    Consumer<C> undo,
                    BiFunction<C, C, Optional<C>> merge) {
                return fixedSizeHistoryUndoManager(changeStream, apply, undo, merge, capacity);
            }
        };
    }

    public static <C> UndoManager zeroHistoryUndoManager(EventStream<C> changeStream) {
        ChangeQueue<C> queue = new ZeroSizeChangeQueue<>();
        return new UndoManagerImpl<>(queue, c -> {}, c -> {}, (c1, c2) -> Optional.empty(), changeStream);
    }

    public static UndoManagerFactory zeroHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Consumer<C> apply,
                    Consumer<C> undo) {
                return zeroHistoryUndoManager(changeStream);
            }

            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Consumer<C> apply,
                    Consumer<C> undo,
                    BiFunction<C, C, Optional<C>> merge) {
                return zeroHistoryUndoManager(changeStream);
            }
        };
    }
}
