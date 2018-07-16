package org.fxmisc.undo.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.impl.ChangeQueue.QueuePosition;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;

/**
 * Implementation for {@link UndoManager} for single changes. For multiple changes, see
 * {@link MultiChangeUndoManagerImpl}.
 *
 * @param <C> the type of change to undo/redo
 */
public class UndoManagerImpl<C> implements UndoManager<C> {

    private class UndoPositionImpl implements UndoPosition {
        private final QueuePosition queuePos;

        UndoPositionImpl(QueuePosition queuePos) {
            this.queuePos = queuePos;
        }

        @Override
        public void mark() {
            mark = queuePos;
            canMerge = false;
            atMarkedPosition.invalidate();
        }

        @Override
        public boolean isValid() {
            return queuePos.isValid();
        }
    }

    private final ChangeQueue<C> queue;
    private final Function<? super C, ? extends C> invert;
    private final Consumer<C> apply;
    private final BiFunction<C, C, Optional<C>> merge;
    private final Predicate<C> isIdentity;
    private final Subscription subscription;
    private final SuspendableNo performingAction = new SuspendableNo();

    private final EventSource<Void> invalidationRequests = new EventSource<Void>();

    private final Val<C> nextUndo = new ValBase<C>() {
        @Override protected Subscription connect() { return invalidationRequests.subscribe(x -> invalidate()); }
        @Override protected C computeValue() { return queue.hasPrev() ? queue.peekPrev() : null; }
    };

    private final Val<C> nextRedo = new ValBase<C>() {
        @Override protected Subscription connect() { return invalidationRequests.subscribe(x -> invalidate()); }
        @Override protected C computeValue() { return queue.hasNext() ? queue.peekNext() : null; }
    };

    private final BooleanBinding atMarkedPosition = new BooleanBinding() {
        { invalidationRequests.addObserver(x -> this.invalidate()); }

        @Override
        protected boolean computeValue() {
            return mark.equals(queue.getCurrentPosition());
        }
    };

    private boolean canMerge;
    private QueuePosition mark;
    private C expectedChange = null;

    public UndoManagerImpl(
            ChangeQueue<C> queue,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            EventStream<C> changeSource) {
        this(queue, invert, apply, merge, isIdentity, changeSource, Duration.ZERO);
    }

    public UndoManagerImpl(
            ChangeQueue<C> queue,
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            Predicate<C> isIdentity,
            EventStream<C> changeSource,
            Duration preventMergeDelay) {
        this.queue = queue;
        this.invert = invert;
        this.apply = apply;
        this.merge = merge;
        this.isIdentity = isIdentity;
        this.mark = queue.getCurrentPosition();

        Subscription mainSub = changeSource.subscribe(this::changeObserved);

        if (preventMergeDelay.isZero() || preventMergeDelay.isNegative()) {
            subscription = mainSub;
        } else {
            Subscription sub2 = changeSource.successionEnds(preventMergeDelay).subscribe(ignore -> preventMerge());
            subscription = mainSub.and(sub2);
        }
    }

    @Override
    public void close() {
        subscription.unsubscribe();
    }

    @Override
    public boolean undo() {
        return applyChange(isUndoAvailable(), () -> invert.apply(queue.prev()));
    }

    @Override
    public boolean redo() {
        return applyChange(isRedoAvailable(), queue::next);
    }

    @Override
    public Val<C> nextUndoProperty() {
        return nextUndo;
    }

    @Override
    public Val<C> nextRedoProperty() {
        return nextRedo;
    }

    @Override
    public boolean isUndoAvailable() {
        return nextUndo.isPresent();
    }

    @Override
    public Val<Boolean> undoAvailableProperty() {
        return nextUndo.map(c -> true).orElseConst(false);
    }

    @Override
    public boolean isRedoAvailable() {
        return nextRedo.isPresent();
    }

    @Override
    public Val<Boolean> redoAvailableProperty() {
        return nextRedo.map(c -> true).orElseConst(false);
    }

    @Override
    public boolean isPerformingAction() {
        return performingAction.get();
    }

    @Override
    public ObservableBooleanValue performingActionProperty() {
        return performingAction;
    }

    @Override
    public boolean isAtMarkedPosition() {
        return atMarkedPosition.get();
    }

    @Override
    public ObservableBooleanValue atMarkedPositionProperty() {
        return atMarkedPosition;
    }

    @Override
    public UndoPosition getCurrentPosition() {
        return new UndoPositionImpl(queue.getCurrentPosition());
    }

    @Override
    public void preventMerge() {
        canMerge = false;
    }

    @Override
    public void forgetHistory() {
        queue.forgetHistory();
        invalidateProperties();
    }

    /**
     * Helper method for reducing code duplication
     *
     * @param isChangeAvailable same as `isUndoAvailable()` [Undo] or `isRedoAvailable()` [Redo]
     * @param changeToApply same as `invert.apply(queue.prev())` [Undo] or `queue.next()` [Redo]
     */
    private boolean applyChange(boolean isChangeAvailable, Supplier<C> changeToApply) {
        if (isChangeAvailable) {
            canMerge = false;

            // perform change
            C change = changeToApply.get();
            this.expectedChange = change;
            performingAction.suspendWhile(() -> apply.accept(change));
            if(this.expectedChange != null) {
                throw new IllegalStateException("Expected change not received:\n"
                        + this.expectedChange);
            }

            invalidateProperties();
            return true;
        } else {
            return false;
        }
    }

    private void changeObserved(C change) {
        if(expectedChange == null) {
            if (!isIdentity.test(change)) {
                addChange(change);
            }
        } else if(expectedChange.equals(change)) {
            expectedChange = null;
        } else {
            throw new IllegalArgumentException("Unexpected change received."
                    + "\nExpected:\n" + expectedChange
                    + "\nReceived:\n" + change);
        }
    }

    @SuppressWarnings("unchecked")
    private void addChange(C change) {
        if(canMerge && queue.hasPrev()) {
            C prev = queue.prev();

            // attempt to merge the changes
            Optional<C> merged = merge.apply(prev, change);
            if(merged.isPresent()) {
                if (isIdentity.test(merged.get())) {
                    canMerge = false;
                    queue.push(); // clears the future
                } else {
                    canMerge = true;
                    queue.push(merged.get());
                }
            } else {
                canMerge = true;
                queue.next();
                queue.push(change);
            }
        } else {
            queue.push(change);
            canMerge = true;
        }
        invalidateProperties();
    }

    private void invalidateProperties() {
        invalidationRequests.push(null);
    }
}
