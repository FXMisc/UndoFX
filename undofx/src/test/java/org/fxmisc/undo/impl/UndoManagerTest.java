package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.junit.Test;
import org.reactfx.EventSource;
import org.reactfx.value.Var;

public class UndoManagerTest {

    @Test
    public void testUndoInvertsTheChange() {
        EventSource<Integer> changes = new EventSource<>();
        Var<Integer> lastAction = Var.newSimpleVar(null);
        UndoManager um = UndoManagerFactory.unlimitedHistoryLinearManager(
                changes, i -> -i, lastAction::setValue);

        changes.push(3);
        changes.push(7);
        assertNull(lastAction.getValue());

        um.undo(null);
        assertEquals(-7, lastAction.getValue().intValue());

        um.undo(null);
        assertEquals(-3, lastAction.getValue().intValue());

        um.redo(null);
        assertEquals(3, lastAction.getValue().intValue());

        um.redo(null);
        assertEquals(7, lastAction.getValue().intValue());
    }

    @Test
    public void testMark() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.fixedSizeHistoryLinearManager(
                changes, c -> c, c -> {}, 4);

        assertTrue(um.atMarkedPositionProperty(null).get());
        changes.push(1);
        assertFalse(um.atMarkedPositionProperty(null).get());
        changes.push(2);
        um.mark(null);
        assertTrue(um.atMarkedPositionProperty(null).get());
        changes.push(3);
        changes.push(4);
        assertFalse(um.atMarkedPositionProperty(null).get());
        um.undo(null);
        um.undo(null);
        assertTrue(um.atMarkedPositionProperty(null).get());
        changes.push(3);
        changes.push(4);
        changes.push(5); // overflow
        changes.push(6);
        assertFalse(um.atMarkedPositionProperty(null).get());
    }

    @Test
    public void zeroHistoryUndoManagerMark() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.zeroHistoryUndoManager(changes);

        assertTrue(um.atMarkedPositionProperty(null).get());
        changes.push(1);
        assertFalse(um.atMarkedPositionProperty(null).get());
        changes.push(2);
        um.mark(null);
        assertTrue(um.atMarkedPositionProperty(null).get());
        changes.push(3);
        changes.push(4);
        assertFalse(um.atMarkedPositionProperty(null).get());
    }

    /**
     * Tests that isAtMarkedPosition() forces atMarkedPositionProperty()
     * become valid.
     */
    @Test
    public void testAtMarkedPositionRevalidation() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.zeroHistoryUndoManager(changes);

        um.atMarkedPositionProperty(null).get(); // atMarkedPositionProperty is now valid

        // we are going to expect two invalidations
        CountDownLatch latch = new CountDownLatch(2);
        um.atMarkedPositionProperty(null).addListener(observable -> latch.countDown());

        changes.push(1); // atMarkedPositionProperty has been invalidated
        assertEquals(1, latch.getCount());

        um.isAtMarkedPosition(null); // we want to test whether this caused revalidation of atMarkedPositionProperty

        changes.push(2); // should have caused invalidation of atMarkedPositionProperty
        assertEquals(0, latch.getCount());
    }
}
