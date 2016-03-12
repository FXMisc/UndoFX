package org.fxmisc.undo.impl;

import java.util.HashMap;
import javafx.beans.value.ObservableBooleanValue;
import org.fxmisc.undo.UndoManager;
import org.reactfx.Guard;
import org.reactfx.SuspendableNo;

public class NonLinearUndoManager<Source, Change> implements UndoManager<Source> {

    private final HashMap<Source, LinearUndoManager<Change>> map;

    private final SuspendableNo performingAction = new SuspendableNo();

    public NonLinearUndoManager(Source src, LinearUndoManager<Change> firstManager, int size, float loadFactor) {
        map = new HashMap<>(size, loadFactor);
        map.put(src, firstManager);
    }

    public NonLinearUndoManager(Source src, LinearUndoManager<Change> firstManager, int size) {
        map = new HashMap<>(size); // use default load factor (0.75)
        map.put(src, firstManager);
    }

    public NonLinearUndoManager(Source src, LinearUndoManager<Change> firstManager) {
        this(src, firstManager, 3);
    }

    public void add(Source src, LinearUndoManager<Change> manager) {
        map.put(src, manager);
    }

    public int size() {
        return map.keySet().size();
    }

    @Override
    public void close(Source src) {
        map.get(src).close();
    }

    @Override
    public void close() {
        map.values().forEach(LinearUndoManager::close);
    }

    @Override
    public boolean undo(Source src) {
        boolean result;
        try (Guard g = performingAction.suspend()) {
            result = map.get(src).undo(null);
        }
        return result;
    }

    @Override
    public boolean redo(Source src) {
        boolean result;
        try (Guard g = performingAction.suspend()) {
            result = map.get(src).redo(null);
        }
        return result;
    }

    @Override
    public boolean isUndoAvailable(Source src) {
        return map.get(src).isUndoAvailable(null);
    }

    @Override
    public ObservableBooleanValue undoAvailableProperty(Source src) {
        return map.get(src).undoAvailableProperty(null);
    }

    @Override
    public boolean isRedoAvailable(Source src) {
        return map.get(src).isRedoAvailable(null);
    }

    @Override
    public ObservableBooleanValue redoAvailableProperty(Source src) {
        return map.get(src).redoAvailableProperty(null);
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
    public boolean isAtMarkedPosition(Source src) {
        return map.get(src).isAtMarkedPosition(null);
    }

    @Override
    public ObservableBooleanValue atMarkedPositionProperty(Source src) {
        return map.get(src).atMarkedPositionProperty(null);
    }

    @Override
    public UndoPosition getCurrentPosition(Source src) {
        return map.get(src).getCurrentPosition(null);
    }

    @Override
    public void mark(Source src) {
        map.get(src).mark(null);
    }

    @Override
    public void preventMerge(Source src) {
        map.get(src).preventMerge(null);
    }

    @Override
    public void forgetHistory(Source src) {
        map.get(src).forgetHistory(null);
    }

}
