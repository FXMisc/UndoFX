package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import org.fxmisc.undo.impl.ChangeQueue.QueuePosition;
import org.junit.Test;

public class FixedSizeChangeQueueTest {

    @Test
    public void testOverflow() {
        ChangeQueue<Integer> queue = new FixedSizeChangeQueue<>(5);
        queue.push(1, 2, 3);
        queue.push(4, 5, 6, 7, 8, 9);

        assertFalse(queue.hasNext());
        assertTrue(queue.hasPrev());
        assertEquals(Integer.valueOf(9), queue.prev());
        assertTrue(queue.hasNext());
        assertEquals(Integer.valueOf(8), queue.prev());
        assertEquals(Integer.valueOf(7), queue.prev());
        assertEquals(Integer.valueOf(6), queue.prev());
        assertEquals(Integer.valueOf(5), queue.prev());
        assertFalse(queue.hasPrev());
        assertTrue(queue.hasNext());
    }

    @Test
    public void testPositionValidityOnOverflow() {
        // create empty queue
        ChangeQueue<Integer> queue = new FixedSizeChangeQueue<>(1);

        // check that the initial position is valid
        QueuePosition pos0 = queue.getCurrentPosition();
        assertTrue(pos0.isValid());

        // push first element
        queue.push(1);

        // check that the initial position is still valid
        // and that the current position is valid as well
        assertTrue(pos0.isValid());
        QueuePosition pos1 = queue.getCurrentPosition();
        assertTrue(pos1.isValid());

        // push one more element
        queue.push(2);

        // check that initial position is now invalid,
        // previous position is still valid and
        // the current position is valid as well
        assertFalse(pos0.isValid());
        assertTrue(pos1.isValid());
        QueuePosition pos2 = queue.getCurrentPosition();
        assertTrue(pos2.isValid());

        // push two elements at once
        queue.push(3, 4);

        // check that all previous positions are invalid
        assertFalse(pos0.isValid());
        assertFalse(pos1.isValid());
        assertFalse(pos2.isValid());
    }

    @Test
    public void testPositionValidityOnUndo() {
        ChangeQueue<Integer> queue = new FixedSizeChangeQueue<>(4);
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
        ChangeQueue<Integer> queue = new FixedSizeChangeQueue<>(4);
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
        ChangeQueue<Integer> queue = new FixedSizeChangeQueue<>(2);
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
