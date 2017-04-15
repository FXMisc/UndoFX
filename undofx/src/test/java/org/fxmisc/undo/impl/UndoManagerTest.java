package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javafx.beans.property.SimpleIntegerProperty;
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
        UndoManager um = UndoManagerFactory.unlimitedHistoryUndoManager(
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
        UndoManager um = UndoManagerFactory.fixedSizeHistoryUndoManager(
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

    /**
     * Tests that isAtMarkedPosition() forces atMarkedPositionProperty()
     * become valid.
     */
    @Test
    public void testAtMarkedPositionRevalidation() {
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.zeroHistoryUndoManager(changes);

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
        UndoManager um = UndoManagerFactory.unlimitedHistoryUndoManager(
                changes, i -> -i, i -> {});

        changes.push(1);

        um.undo(); // should throw because the undone change is not received back
    }

    // Identity Change Tests

    @Test
    public void testPushedNonIdentityChangeIsStored() {
        SimpleIntegerProperty lastAppliedValue = new SimpleIntegerProperty(0);
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.unlimitedHistoryUndoManager(
                changes,
                i -> -i,    // invert
                i -> { lastAppliedValue.set(i); changes.push(i); }, // apply change and re-emit value so expected change is received
                (a, b) -> Optional.of(a + b), // merge adds two changes together
                i -> i == 0); // identity change = 0

        changes.push(4);
        assertTrue(um.isUndoAvailable());
        um.undo();
        assertEquals(-4, lastAppliedValue.get());
        assertFalse(um.isUndoAvailable());
    }

    @Test
    public void testPushedIdentityChangeIsNotStored() {
        SimpleIntegerProperty lastAppliedValue = new SimpleIntegerProperty(0);
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.unlimitedHistoryUndoManager(
                changes,
                i -> -i,    // invert
                i -> { lastAppliedValue.set(i); changes.push(i); }, // apply change and re-emit value so expected change is received
                (a, b) -> Optional.of(a + b), // merge adds two changes together
                i -> i == 0); // identity change = 0

        // force lastAppliedValue to store non-zero value
        changes.push(4);
        um.undo();

        // test that pushed identity change is not stored
        changes.push(0);
        assertFalse(um.isUndoAvailable());
        assertEquals(-4, lastAppliedValue.get());
    }

    @Test
    public void testMergeResultingInIdentityChangeAnnihilatesBothAndPreventsNextMerge() {
        SimpleIntegerProperty lastAppliedValue = new SimpleIntegerProperty(0);
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.unlimitedHistoryUndoManager(
                changes,
                i -> -i,    // invert
                i -> { lastAppliedValue.set(i); changes.push(i); }, // apply change and re-emit value so expected change is received
                (a, b) -> Optional.of(a + b), // merge adds two changes together
                i -> i == 0); // identity change = 0

        // have at least one change stored
        changes.push(6);
        // prevent next merge from occurring
        um.preventMerge();

        // now push the identity-resulting merge changes
        changes.push(-3);   // change A
        changes.push(3);    // change B

        // changes should annihilate; neither are stored
        assertTrue(um.isUndoAvailable());
        um.undo();
        assertFalse(um.isUndoAvailable());
        assertEquals(-6, lastAppliedValue.get());

        um.redo(); // redo to test whether merge occurs on next push
        changes.push(3);
        assertTrue(um.isUndoAvailable());
        um.undo();
        assertTrue(um.isUndoAvailable());
        assertEquals(-3, lastAppliedValue.get());
    }

    @Test
    public void testMergeResultingInNonIdentityChangeStoresMergeAndPreventsNextMerge() {
        SimpleIntegerProperty lastAppliedValue = new SimpleIntegerProperty(0);
        EventSource<Integer> changes = new EventSource<>();
        UndoManager um = UndoManagerFactory.unlimitedHistoryUndoManager(
                changes,
                i -> -i,    // invert
                i -> { lastAppliedValue.set(i); changes.push(i); }, // apply change and re-emit value so expected change is received
                (a, b) -> Optional.of(a + b), // merge adds two changes together
                i -> i == 0); // identity change = 0

        changes.push(1);
        changes.push(2);
        assertTrue(um.isUndoAvailable());
        um.undo();
        assertFalse(um.isUndoAvailable());
        assertEquals(-3, lastAppliedValue.get());

        um.redo(); // redo to test whether merge occurs on next push
        changes.push(5);
        assertTrue(um.isUndoAvailable());
        um.undo();
        assertEquals(-5, lastAppliedValue.get());
    }
}
