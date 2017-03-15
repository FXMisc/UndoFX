package org.fxmisc.undo.impl.linear;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.LinearUndoManagerFactory;
import org.junit.Test;
import org.reactfx.EventSource;
import org.reactfx.value.Var;

public class LinearUndoManagerTest {

    @Test
    public void testUndoInvertsTheChange() {
        EventSource<Integer> changes = new EventSource<>();
        Var<Integer> lastAction = Var.newSimpleVar(null);
        UndoManager um = LinearUndoManagerFactory.unlimitedHistoryUndoManager(
                changes, i -> -i, i -> { lastAction.setValue(i); changes.push(i); });

        changes.push(3);
        changes.push(7);
        assertNull(lastAction.getValue());

        um.undo();
        assertEquals(-7, lastAction.getValue().intValue());

        um.undo();
        assertEquals(-3, lastAction.getValue().intValue());

        um.redo();
        assertEquals(3, lastAction.getValue().intValue());

        um.redo();
        assertEquals(7, lastAction.getValue().intValue());
    }

    @Test
    public void testMark() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = LinearUndoManagerFactory.fixedSizeHistoryUndoManager(
                changes, c -> c, changes::push, 4);

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
        UndoManager um = LinearUndoManagerFactory.zeroHistoryUndoManager(changes);

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

    /**
     * Tests that isAtMarkedPosition() forces atMarkedPositionProperty()
     * become valid.
     */
    @Test
    public void testAtMarkedPositionRevalidation() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = LinearUndoManagerFactory.zeroHistoryUndoManager(changes);

        um.atMarkedPositionProperty().get(); // atMarkedPositionProperty is now valid

        // we are going to expect two invalidations
        CountDownLatch latch = new CountDownLatch(2);
        um.atMarkedPositionProperty().addListener(observable -> latch.countDown());

        changes.push(1); // atMarkedPositionProperty has been invalidated
        assertEquals(1, latch.getCount());

        um.isAtMarkedPosition(); // we want to test whether this caused revalidation of atMarkedPositionProperty

        changes.push(2); // should have caused invalidation of atMarkedPositionProperty
        assertEquals(0, latch.getCount());
    }

    @Test(expected = IllegalStateException.class)
    public void testFailFastWhenExpectedChangeNotReceived() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = LinearUndoManagerFactory.unlimitedHistoryUndoManager(
                changes, i -> -i, i -> {});

        changes.push(1);

        um.undo(); // should throw because the undone change is not received back
    }
}
