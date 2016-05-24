package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import org.fxmisc.undo.impl.LinearChangeQueue.QueuePosition;
import org.junit.Test;

public class UnlimitedLinearChangeQueueTest {

    @Test
    public void testPositionValidityOnUndo() {
        LinearChangeQueue<Integer> queue = new UnlimitedLinearChangeQueue<>();
        QueuePosition pos0 = queue.getCurrentPosition();
        queue.push(1);
        QueuePosition pos1 = queue.getCurrentPosition();
        queue.push(2);
        QueuePosition pos2 = queue.getCurrentPosition();
        queue.push(3);
        QueuePosition pos3 = queue.getCurrentPosition();
        queue.push(4);
        QueuePosition pos4 = queue.getCurrentPosition();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());

        queue.prev();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());

        queue.push(4);

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertFalse(pos4.isValid());

        queue.prev();
        queue.prev();
        queue.prev();
        queue.prev();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertFalse(pos4.isValid());

        queue.push(1);

        assertTrue(pos0.isValid());
        assertFalse(pos1.isValid());
        assertFalse(pos2.isValid());
        assertFalse(pos3.isValid());
        assertFalse(pos4.isValid());
    }

    @Test
    public void testPositionValidityOnForgetHistory() {
        LinearChangeQueue<Integer> queue = new UnlimitedLinearChangeQueue<>();
        QueuePosition pos0 = queue.getCurrentPosition();
        queue.push(1);
        QueuePosition pos1 = queue.getCurrentPosition();
        queue.push(2);
        QueuePosition pos2 = queue.getCurrentPosition();
        queue.push(3);
        QueuePosition pos3 = queue.getCurrentPosition();
        queue.push(4);
        QueuePosition pos4 = queue.getCurrentPosition();

        queue.prev();
        queue.prev();
        queue.forgetHistory();

        assertFalse(pos0.isValid());
        assertFalse(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());
    }

    @Test
    public void testPositionEquality() {
        LinearChangeQueue<Integer> queue = new UnlimitedLinearChangeQueue<>();
        queue.push(1);
        QueuePosition pos = queue.getCurrentPosition();
        assertEquals(pos, queue.getCurrentPosition());
        queue.push(2);
        assertNotEquals(pos, queue.getCurrentPosition());
        queue.prev();
        assertEquals(pos, queue.getCurrentPosition());
        queue.prev();
        assertNotEquals(pos, queue.getCurrentPosition());
        queue.next();
        assertEquals(pos, queue.getCurrentPosition());
        queue.prev();
        queue.push(3);
        assertNotEquals(pos, queue.getCurrentPosition());
    }
}
