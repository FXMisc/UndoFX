package org.fxmisc.undo;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.FixedSizeChangeQueue;
import org.fxmisc.undo.impl.MultiChangeUndoManagerImpl;
import org.fxmisc.undo.impl.UndoManagerImpl;
import org.fxmisc.undo.impl.UnlimitedChangeQueue;
import org.fxmisc.undo.impl.ZeroSizeChangeQueue;
import org.reactfx.EventStream;

public interface UndoManagerFactory {

    /* ******************************* *
     * Single Change                   *
     * ******************************* */

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
    default <C> UndoManager<C> createSingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return createSingleChangeUM(changeStream, invert, apply, (c1, c2) -> Optional.empty());
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
    default <C> UndoManager<C> createSingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return createSingleChangeUM(changeStream, invert, apply, merge, c -> false);
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
    default  <C> UndoManager<C> createSingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity) {
        return createSingleChangeUM(changeStream, invert, apply, merge, isIdentity, Duration.ZERO);
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
     * @param preventMergeDelay the amount by which to wait before calling {@link UndoManager#preventMerge()}.
     *                          Helpful when one wants to prevent merges after a period of inactivity (the
     *                          {@code changeStream} doesn't emit an event after the given duration). Passing in
     *                          a negative or zero duration will never call {@link UndoManager#preventMerge()}.
     */
    <C> UndoManager<C> createSingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            Duration preventMergeDelay);

    /* ******************************* *
     * Single Change Implementations   *
     * ******************************* */

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<C> unlimitedHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply) {
        return unlimitedHistorySingleChangeUM(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<C> unlimitedHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return unlimitedHistorySingleChangeUM(changeStream, invert, apply, merge, c -> false, Duration.ZERO);
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<C> unlimitedHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity) {
        return unlimitedHistorySingleChangeUM(changeStream, invert, apply, merge, isIdentity, Duration.ZERO);
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<C> unlimitedHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            Duration preventMergeDelay) {
        ChangeQueue<C> queue = new UnlimitedChangeQueue<C>();
        return new UndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream, preventMergeDelay);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<C> fixedSizeHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            int capacity) {
        return fixedSizeHistorySingleChangeUM(changeStream, invert, apply, (c1, c2) -> Optional.empty(), capacity);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<C> fixedSizeHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        return fixedSizeHistorySingleChangeUM(changeStream, invert, apply, merge, c -> false, Duration.ZERO, capacity);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see
     * {@link #createSingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<C> fixedSizeHistorySingleChangeUM(
            EventStream<C> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            Duration preventMergeDelay,
            int capacity) {
        ChangeQueue<C> queue = new FixedSizeChangeQueue<C>(capacity);
        return new UndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream, preventMergeDelay);
    }

    /**
     * Creates an {@link UndoManager} with no history: all changes emitted from {@code changeStream} will be
     * immediately forgotten. Therefore, the returned {@linkplain UndoManager} will never be able to undo/redo
     * any change emitted from {@code changeStream}.
     * However, the (imaginary) current position will keep advancing, so that one can still use
     * {@link UndoManager#atMarkedPositionProperty()} to determine whether any change has occurred since the last
     * {@link UndoManager#mark()} (e.g. since the last save).
     */
    public static <C> UndoManager<C> zeroHistorySingleChangeUM(EventStream<C> changeStream) {
        ChangeQueue<C> queue = new ZeroSizeChangeQueue<>();
        return new UndoManagerImpl<>(queue, c -> c, c -> {}, (c1, c2) -> Optional.empty(), c -> false, changeStream);
    }

    /* ******************************* *
     * Multi Change                    *
     * ******************************* */

    /**
     * Creates an {@link UndoManager} that tracks lists of changes emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a list of changes. {@code list} describes a list of actions to be performed.
     *              Calling {@code apply.accept(list)} <em>must</em> cause {@code list} to be emitted from
     *              the {@code changeStream}.
     */
    default <C> UndoManager<List<C>> createMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply) {
        return createMultiChangeUM(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} that tracks and optionally merges lists of changes
     * emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a list of changes. {@code list} describes a list of actions to be performed.
     *              Calling {@code apply.accept(list)} <em>must</em> cause {@code list} to be emitted from
     *              the {@code changeStream}.
     * @param merge Used to merge two subsequent changes into one.
     *              Returns an empty {@linkplain Optional} when the changes cannot or should not be merged.
     */
    default <C> UndoManager<List<C>> createMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return createMultiChangeUM(changeStream, invert, apply, merge, c -> false);
    }

    /**
     * Creates an {@link UndoManager} that tracks and optionally merges lists of changes
     * emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a list of changes. {@code list} describes a list of actions to be performed.
     *              Calling {@code apply.accept(list)} <em>must</em> cause {@code list} to be emitted from
     *              the {@code changeStream}.
     * @param merge Used to merge two subsequent changes into one.
     *              Returns an empty {@linkplain Optional} when the changes cannot or should not be merged.
     *              If two changes "annihilate" (i.e. {@code merge.apply(c1, c2).isPresen()} and
     *              {@code isIdentity.test(merge.apply(c1, c2).get())} are both {@code true}), it should
     *              be the case that one is inverse of the other ({@code invert.apply(c1).equals(c2)}).
     * @param isIdentity returns true for changes whose application would have no effect, thereby equivalent
     *                   to an identity function ({@link Function#identity()}) on the underlying model.
     */
    default  <C> UndoManager<List<C>> createMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity) {
        return createMultiChangeUM(changeStream, invert, apply, merge, isIdentity, Duration.ZERO);
    }

    /**
     * Creates an {@link UndoManager} that tracks and optionally merges lists of changes
     * emitted from {@code changeStream}.
     *
     * @param <C> representation of a change
     * @param invert Inverts a change, so that applying the inverted change ({@code apply.accept(invert.apply(c))})
     *               has the effect of undoing the original change ({@code c}). Inverting a change twice should
     *               result in the original change ({@code invert.apply(invert.apply(c)).equals(c)}).
     * @param apply Used to apply a list of changes. {@code list} describes a list of actions to be performed.
     *              Calling {@code apply.accept(list)} <em>must</em> cause {@code list} to be emitted from
     *              the {@code changeStream}.
     * @param merge Used to merge two subsequent changes into one.
     *              Returns an empty {@linkplain Optional} when the changes cannot or should not be merged.
     *              If two changes "annihilate" (i.e. {@code merge.apply(c1, c2).isPresen()} and
     *              {@code isIdentity.test(merge.apply(c1, c2).get())} are both {@code true}), it should
     *              be the case that one is inverse of the other ({@code invert.apply(c1).equals(c2)}).
     * @param isIdentity returns true for changes whose application would have no effect, thereby equivalent
     *                   to an identity function ({@link Function#identity()}) on the underlying model.
     * @param preventMergeDelay the amount by which to wait before calling {@link UndoManager#preventMerge()}.
     *                          Helpful when one wants to prevent merges after a period of inactivity (the
     *                          {@code changeStream} doesn't emit an event after the given duration). Passing in
     *                          a negative or zero duration will never call {@link UndoManager#preventMerge()}.
     */
    <C> UndoManager<List<C>> createMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            Duration preventMergeDelay);

    /* ******************************* *
     * Multi Change Implementations    *
     * ******************************* */

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<List<C>> unlimitedHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply) {
        return unlimitedHistoryMultiChangeUM(changeStream, invert, apply, (c1, c2) -> Optional.empty());
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<List<C>> unlimitedHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge) {
        return unlimitedHistoryMultiChangeUM(changeStream, invert, apply, merge, c -> false, Duration.ZERO);
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<List<C>> unlimitedHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity) {
        return unlimitedHistoryMultiChangeUM(changeStream, invert, apply, merge, isIdentity, Duration.ZERO);
    }

    /**
     * Creates an {@link UndoManager} with unlimited history.
     *
     * For description of parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.
     */
    public static <C> UndoManager<List<C>> unlimitedHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            Duration preventMergeDelay) {
        ChangeQueue<List<C>> queue = new UnlimitedChangeQueue<>();
        return new MultiChangeUndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream, preventMergeDelay);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<List<C>> fixedSizeHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            int capacity) {
        return fixedSizeHistoryMultiChangeUM(changeStream, invert, apply, (c1, c2) -> Optional.empty(), capacity);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<List<C>> fixedSizeHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge,
            int capacity) {
        return fixedSizeHistoryMultiChangeUM(changeStream, invert, apply, merge, c -> false, Duration.ZERO, capacity);
    }

    /**
     * Creates an {@link UndoManager} with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * <p>For description of the remaining parameters, see
     * {@link #createMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)}.</p>
     *
     * @param capacity maximum number of changes the returned UndoManager can store
     */
    public static <C> UndoManager<List<C>> fixedSizeHistoryMultiChangeUM(
            EventStream<List<C>> changeStream,
            Function<? super C, ? extends C> invert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            Duration preventMergeDelay,
            int capacity) {
        ChangeQueue<List<C>> queue = new FixedSizeChangeQueue<>(capacity);
        return new MultiChangeUndoManagerImpl<>(queue, invert, apply, merge, isIdentity, changeStream, preventMergeDelay);
    }

    /**
     * Creates an {@link UndoManager} with no history: all changes emitted from {@code changeStream} will be
     * immediately forgotten. Therefore, the returned {@linkplain UndoManager} will never be able to undo/redo
     * any change emitted from {@code changeStream}.
     * However, the (imaginary) current position will keep advancing, so that one can still use
     * {@link UndoManager#atMarkedPositionProperty()} to determine whether any change has occurred since the last
     * {@link UndoManager#mark()} (e.g. since the last save).
     */
    public static <C> UndoManager<List<C>> zeroHistoryMultiChangeUM(EventStream<List<C>> changeStream) {
        ChangeQueue<List<C>> queue = new ZeroSizeChangeQueue<>();
        Consumer<List<C>> doNothingForUndoAndRedo = c -> {};
        return new MultiChangeUndoManagerImpl<>(queue, c -> c, doNothingForUndoAndRedo,
                (c1, c2) -> Optional.empty(), c -> false, changeStream);
    }

    /* ******************************* *
     * Factories                       *
     * ******************************* */

    /**
     * Creates a factory for {@link UndoManager}s with unlimited history.
     *
     * @see #unlimitedHistorySingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)
     * @see #unlimitedHistoryMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration)
     */
    public static UndoManagerFactory unlimitedHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager<C> createSingleChangeUM(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity,
                    Duration preventMergeDelay) {
                return unlimitedHistorySingleChangeUM(changeStream, invert, apply, merge, isIdentity, preventMergeDelay);
            }

            @Override
            public <C> UndoManager<List<C>> createMultiChangeUM(
                    EventStream<List<C>> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<List<C>> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity,
                    Duration preventMergeDelay) {
                return unlimitedHistoryMultiChangeUM(changeStream, invert, apply, merge, isIdentity, preventMergeDelay);
            }
        };
    }

    /**
     * Creates a factory for {@link UndoManager}s with bounded history.
     * When at full capacity, a new change will cause the oldest change to be forgotten.
     *
     * @see #fixedSizeHistorySingleChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration, int)
     * @see #fixedSizeHistoryMultiChangeUM(EventStream, Function, Consumer, BiFunction, Predicate, Duration, int)
     */
    public static UndoManagerFactory fixedSizeHistoryFactory(int capacity) {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager<C> createSingleChangeUM(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity,
                    Duration preventMergeDelay) {
                return fixedSizeHistorySingleChangeUM(changeStream, invert, apply, merge, isIdentity, preventMergeDelay, capacity);
            }

            @Override
            public <C> UndoManager<List<C>> createMultiChangeUM(
                    EventStream<List<C>> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<List<C>> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity,
                    Duration preventMergeDelay) {
                return fixedSizeHistoryMultiChangeUM(changeStream, invert, apply, merge, isIdentity, preventMergeDelay, capacity);
            }
        };
    }

    /**
     * Creates a factory for {@link UndoManager}s with no history.
     *
     * @see #zeroHistorySingleChangeUM(EventStream)
     * @see #zeroHistoryMultiChangeUM(EventStream)
     */
    public static UndoManagerFactory zeroHistoryFactory() {
        return new UndoManagerFactory() {
            @Override
            public <C> UndoManager<C> createSingleChangeUM(
                    EventStream<C> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<C> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity,
                    Duration preventMergeDelay) {
                return zeroHistorySingleChangeUM(changeStream);
            }

            @Override
            public <C> UndoManager<List<C>> createMultiChangeUM(
                    EventStream<List<C>> changeStream,
                    Function<? super C, ? extends C> invert,
                    Consumer<List<C>> apply,
                    BiFunction<C, C, Optional<C>> merge,
                    Predicate<C> isIdentity,
                    Duration preventMergeDelay) {
                return zeroHistoryMultiChangeUM(changeStream);
            }
        };
    }
}
