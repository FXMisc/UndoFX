package org.fxmisc.undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.FixedSizeChangeQueue;
import org.fxmisc.undo.impl.UndoManagerImpl;
import org.fxmisc.undo.impl.UnlimitedChangeQueue;
import org.fxmisc.undo.impl.ZeroSizeChangeQueue;
import org.reactfx.EventStream;

public interface UndoManagerFactory {

    /**
     * Creates an {@link UndoManager} where changes will never be merged and every change emitted from the
     * changeStream is considered to be a non-identity change.
     *
     * @param <C> the type of change object to use
     */
    default <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return create(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} where every change emitted from the changeStream is considered to be a
     * non-identity change
     *
     * @param <C> the type of change object to use
     */
    default <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return create(changeStream, invert, apply, merge, c -> false);
    }

    /**
     * Creates an {@link UndoManager}.
     *
     * @param merge merges the next undo and the most recently pushed change. If the resulting change is an identity
     *              change, neither of the changes will be stored (even the next undo).
     * @param isIdentity determines whether change is an identity change (e.g. {@link Function#identity()}). For
     *                   example, {@code 0} is the identity change in
     *                   {@code BiFunction<Integer, Integer, Integer> plus = (i, j) -> i + j} because
     *                   {@code 4 == 4 + 0 == plus.apply(4, 0)}
     * @param <C> the type of change object to use
     */
    <C> UndoManager create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity);

    /**
     * Creates an {@link UndoManager} with an unlimited history where no changes will ever be merged and every change
     * emitted by the changeStream is considered a non-identity change.
     *
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return unlimitedHistoryUndoManager(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} with an unlimited history where every change emitted by the changeStream
     * is considered a non-identity change
     *
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return unlimitedHistoryUndoManager(changeStream, invert, apply, merge, c -> false);
    }
    /**
     * Creates an {@link UndoManager} with an unlimited history.
     *
     * @param merge merges the next undo and the most recently pushed change. If the resulting change is an identity
     *              change, neither of the changes will be stored.
     * @param isIdentity determines whether change is an identity change (e.g. {@link Function#identity()}). For
     *                   example, {@code 0} is the identity change in
     *                   {@code BiFunction<Integer, Integer, Integer> plus = (i, j) -> i + j} because
     *                   {@code 4 == 4 + 0 == plus.apply(4, 0)}
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        return new UndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream);
    }

    /**
     * Creates an {@link UndoManagerFactory} whose {@code create} methods will create an {@link UndoManager} with
     * an unlimited history.
     */
    public static UndoManagerFactory unlimitedHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity) {
                return unlimitedHistoryUndoManager(changeStream, invert, apply, merge, isIdentity);
            }
        };
    }

    /**
     * Creates an {@link UndoManager} with a limited history where changes are never merged and every change emitted
     * from the changeStream is considered to be a non-identity change; when at full capacity, a new change will
     * cause the oldest change to be forgotten.
     *
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            int capacity) {
        return fixedSizeHistoryUndoManager(changeStream, invert, apply, (c1, c2) -> Optional.empty(), capacity);
    }

    /**
     * Creates an {@link UndoManager} with a limited history where every change emitted from the changeStream
     * is considered to be a non-identity change; when at full capacity, a new change will cause the oldest
     * change to be forgotten.
     *
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        return fixedSizeHistoryUndoManager(changeStream, invert, apply, merge, c -> false, capacity);
    }

    /**
     * Creates an {@link UndoManager} with a limited history; when at full capacity, a new change will cause the
     * oldest change to be forgotten.
     *
     * @param merge merges the next undo and the most recently pushed change. If the resulting change is an identity
     *              change, neither of the changes will be stored.
     * @param isIdentity determines whether change is an identity change (e.g. {@link Function#identity()}). For
     *                   example, {@code 0} is the identity change in
     *                   {@code BiFunction<Integer, Integer, Integer> plus = (i, j) -> i + j} because
     *                   {@code 4 == 4 + 0 == plus.apply(4, 0)}
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            int capacity) {
        ChangeQueue<C> queue = new FixedSizeChangeQueue<C>(capacity);
        return new UndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream);
    }

    /**
     * Creates an {@link UndoManagerFactory} whose {@code create} methods will create an {@link UndoManager} with
     * a limited history; when at full capacity, a new change will cause the oldest change to be forgotten.
     */
    public static UndoManagerFactory fixedSizeHistoryFactory(int capacity) {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity) {
                return fixedSizeHistoryUndoManager(changeStream, invert, apply, merge, isIdentity, capacity);
            }
        };
    }

    /**
     * Creates an {@link UndoManager} with no history where all changes emitted from the EventStream will be
     * considered non-identity changes and each emitted change will change the
     * {@link UndoManager#atMarkedPositionProperty()}; the {@link UndoManager} will never be able to undo/redo a change
     * emitted from the changeStream.
     *
     * @param <C> the type of change object to use
     */
    public static <C> UndoManager zeroHistoryUndoManager(EventStream<C> changeStream) {
        ChangeQueue<C> queue = new ZeroSizeChangeQueue<>();
        return new UndoManagerImpl<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), c -> false, changeStream);
    }

    /**
     * Creates an {@link UndoManagerFactory} whose {@code create} methods will create an {@link UndoManager} with
     * no history: all changes will change the {@link UndoManager#atMarkedPositionProperty()}, but
     * it will never be able to undo/redo a change emitted from the changeStream.
     */
    public static UndoManagerFactory zeroHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager create(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity) {
                return zeroHistoryUndoManager(changeStream);
            }
        };
    }
}
