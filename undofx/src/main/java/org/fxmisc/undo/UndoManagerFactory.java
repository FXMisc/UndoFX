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
     * Creates an {@link UndoManager} that tracks changes emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a change. From the point of view of {@code apply}, {@code C}
     *              describes an action to be performed. Calling {@code apply.accept(c)}
     *              <em>must</em> cause {@code c} to be emitted from {@code changeStream}.
     */
    default <C> UndoManager<C> create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return create(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} that tracks and optionally merges changes emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a change. From the point of view of {@code apply}, {@code C}
     *              describes an action to be performed. Calling {@code apply.accept(c)}
     *              <em>must</em> cause {@code c} to be emitted from {@code changeStream}.
     * @param merge Used to merge two subsequent changes into one.
     *              Returns an empty {@linkplain Optional} when the changes cannot or should not be merged.
     */
    default <C> UndoManager<C> create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return create(changeStream, invert, apply, merge, c -> false);
    }

    /**
     * Creates an {@link UndoManager} that tracks and optionally merges changes emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a change. From the point of view of {@code apply}, {@code C}
     *              describes an action to be performed. Calling {@code apply.accept(c)}
     *              <em>must</em> cause {@code c} to be emitted from {@code changeStream}.
     * @param merge Used to merge two subsequent changes into one.
     *              Returns an empty {@linkplain Optional} when the changes cannot or should not be merged.
     *              If two changes "annihilate" (i.e. {@code merge.apply(c1, c2).isPresen()} and
     *              {@code isIdentity.test(merge.apply(c1, c2).get())} are both {@code true}), it should
     *              be the case that one is inverse of the other ({@code invert.apply(c1).equals(c2)}).
     * @param isIdentity returns true for changes whose application would have no effect, thereby equivalent
     *                   to an identity function ({@link Function#identity()}) on the underlying model.
     */
    <C> UndoManager<C> create(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity);

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see {@link #create(EventStream, Function, Consumer)}.
     */
    public static <C> UndoManager<C> unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return unlimitedHistoryUndoManager(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see {@link #create(EventStream, Function, Consumer, BiFunction)}.
     */
    public static <C> UndoManager<C> unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return unlimitedHistoryUndoManager(changeStream, invert, apply, merge, c -> false);
    }
    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see {@link #create(EventStream, Function, Consumer, BiFunction, Predicate)}.
     */
    public static <C> UndoManager<C> unlimitedHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        return new UndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream);
    }

    /**
     * Creates a factory for {@link UndoManager}s with unlimited history.
     */
    public static UndoManagerFactory unlimitedHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager<C> create(
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
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see {@link #create(EventStream, Function, Consumer)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<C> fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            int capacity) {
        return fixedSizeHistoryUndoManager(changeStream, invert, apply, (c1, c2) -> Optional.empty(), capacity);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see {@link #create(EventStream, Function, Consumer, BiFunction)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<C> fixedSizeHistoryUndoManager(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        return fixedSizeHistoryUndoManager(changeStream, invert, apply, merge, c -> false, capacity);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see {@link #create(EventStream, Function, Consumer, BiFunction, Predicate)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<C> fixedSizeHistoryUndoManager(
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
     * Creates a factory for {@link UndoManager}s with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     */
    public static UndoManagerFactory fixedSizeHistoryFactory(int capacity) {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager<C> create(
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
     * Creates an {@link UndoManager} with no history: all changes emitted from {@code changeStream} will be
     * immediately forgotten. Therefore, the returned {@linkplain UndoManager} will never be able to undo/redo
     * any change emitted from {@code changeStream}.
     * However, the (imaginary) current position will keep advancing, so that one can still use
     * {@link UndoManager#atMarkedPositionProperty()} to determine whether any change has occurred since the last
     * {@link UndoManager#mark()} (e.g. since the last save).
     */
    public static <C> UndoManager<C> zeroHistoryUndoManager(EventStream<C> changeStream) {
        ChangeQueue<C> queue = new ZeroSizeChangeQueue<>();
        return new UndoManagerImpl<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), c -> false, changeStream);
    }

    /**
     * Creates a factory for {@link UndoManager}s with no history.
     *
     * @see #zeroHistoryUndoManager(EventStream)
     */
    public static UndoManagerFactory zeroHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager<C> create(
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
