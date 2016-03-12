package org.fxmisc.undo.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.value.ObservableBooleanValue;
import org.fxmisc.undo.UndoManager;
import org.reactfx.EventStream;


public class ConsistentNonLinearUndoManager<Source, C> implements UndoManager<Source> {

    // UndoManager implementation: delegate to NonLinearUndoManager
    private final NonLinearUndoManager<Source, C> manager;

    @Override
    public void close() {
        manager.close();
    }

    @Override
    public void close(Source src) {
        manager.close(src);
    }

    @Override
    public boolean undo(Source src) {
        return manager.undo(src);
    }

    @Override
    public boolean redo(Source src) {
        return manager.redo(src);
    }

    @Override
    public boolean isUndoAvailable(Source src) {
        return manager.isUndoAvailable(src);
    }

    @Override
    public ObservableBooleanValue undoAvailableProperty(Source src) {
        return manager.undoAvailableProperty(src);
    }

    @Override
    public boolean isRedoAvailable(Source src) {
        return manager.isRedoAvailable(src);
    }

    @Override
    public ObservableBooleanValue redoAvailableProperty(Source src) {
        return manager.redoAvailableProperty(src);
    }

    @Override
    public boolean isPerformingAction() {
        return manager.isPerformingAction();
    }

    @Override
    public ObservableBooleanValue performingActionProperty() {
        return manager.performingActionProperty();
    }

    @Override
    public boolean isAtMarkedPosition(Source src) {
        return manager.isAtMarkedPosition(src);
    }

    @Override
    public ObservableBooleanValue atMarkedPositionProperty(Source src) {
        return manager.atMarkedPositionProperty(src);
    }

    @Override
    public UndoPosition getCurrentPosition(Source src) {
        return manager.getCurrentPosition(src);
    }

    @Override
    public void mark(Source src) {
        manager.mark(src);
    }

    @Override
    public void preventMerge(Source src) {
        manager.preventMerge(src);
    }

    @Override
    public void forgetHistory(Source src) {
        manager.forgetHistory(src);
    }

    // LinearUndoManager constructor parameters to keep to construct LinearUndoManagers
    //  with the same undo/redo methods.
    private final Function<? super C, ? extends C> invert;
    private final Consumer<C> apply;
    private final BiFunction<C, C, Optional<C>> merge;
    private final EventStream<C> originalChangeSource;

    private LinearUndoManager<C> createLinear(ChangeQueue<C> queue, EventStream<C> changeSource) {
        return new LinearUndoManager<>(queue, invert, apply, merge, changeSource);
    }

    public void addManager(Source src, ChangeQueue<C> queue, EventStream<C> changeSource) {
        manager.add(src, createLinear(queue, changeSource));
    }

    public void addUnlimitedHistoryManager(Source src, EventStream<C> changeSource) {
        manager.add(src, createLinear(new UnlimitedChangeQueue<C>(), changeSource));
    }

    public void addUnlimitedHistoryManager(Source src) {
        addUnlimitedHistoryManager(src, originalChangeSource);
    }

    public void addFixedSizeHistoryManager(Source src, EventStream<C> changeSource, int capacity) {
        manager.add(src, createLinear(new FixedSizeChangeQueue<C>(capacity), changeSource));
    }

    public void addFixedSizeHistoryManager(Source src, int capacity) {
        addFixedSizeHistoryManager(src, originalChangeSource, capacity);
    }

    // Constructors
    private ConsistentNonLinearUndoManager(
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource,
            NonLinearUndoManager<Source, C> manager) {
        this.invert = invert;
        this.apply = apply;
        this.merge = merge;
        this.originalChangeSource = changeSource;
        this.manager = manager;
    }

    public ConsistentNonLinearUndoManager(
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource,
            Source initialSource,
            ChangeQueue<C> queue) {
        this(invert, apply, merge, changeSource, new NonLinearUndoManager<Source, C>(initialSource,
                        new LinearUndoManager<C>(queue, invert, apply, merge, changeSource)
        ));
    }

    public ConsistentNonLinearUndoManager(
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource,
            Source initialSource,
            ChangeQueue<C> queue,
            int capacity) {
        this(invert, apply, merge, changeSource, new NonLinearUndoManager<Source, C>(
                initialSource, new LinearUndoManager<C>(queue, invert, apply, merge, changeSource), capacity
        ));
    }

    public ConsistentNonLinearUndoManager(
            Function<? super C, ? extends C> invert,
            Consumer<C> apply,
            BiFunction<C, C, Optional<C>> merge,
            EventStream<C> changeSource,
            Source initialSource,
            ChangeQueue<C> queue,
            int capacity,
            float loadFactor) {
        this(invert, apply, merge, changeSource, new NonLinearUndoManager<Source, C>(
                initialSource, new LinearUndoManager<C>(queue, invert, apply, merge, changeSource), capacity, loadFactor
        ));
    }
}
