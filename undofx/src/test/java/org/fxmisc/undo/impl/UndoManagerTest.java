package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.junit.Test;
import org.reactfx.EventSource;

public class UndoManagerTest {

    @Test
    public void testMark() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.fixedSizeHistoryUndoManager(
                changes, c -> {}, c -> {}, 4);

        assertTrue(um.atMarkedPositionProperty().get());
        changes.push(1);
        assertFalse(um.atMarkedPositionProperty().get());
        changes.push(2);
        um.mark();
        assertTrue(um.atMarkedPositionProperty().get());
        changes.push(3);
        changes.push(4);
        assertFalse(um.atMarkedPositionProperty().get());
        um.undo();
        um.undo();
        assertTrue(um.atMarkedPositionProperty().get());
        changes.push(3);
        changes.push(4);
        changes.push(5); // overflow
        changes.push(6);
        assertFalse(um.atMarkedPositionProperty().get());
    }

    @Test
    public void zeroHistoryUndoManagerMark() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.zeroHistoryUndoManager(changes);

        assertTrue(um.atMarkedPositionProperty().get());
        changes.push(1);
        assertFalse(um.atMarkedPositionProperty().get());
        changes.push(2);
        um.mark();
        assertTrue(um.atMarkedPositionProperty().get());
        changes.push(3);
        changes.push(4);
        assertFalse(um.atMarkedPositionProperty().get());
    }
}
