package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class ZeroSizeLinearChangeQueueTest {

    @Test
    public void testPositionValidityOnOverflow() {
        LinearChangeQueue<Integer> queue = new ZeroSizeLinearChangeQueue<>();
        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();
        assertTrue(pos0.isValid());
        queue.push(1);
        assertFalse(pos0.isValid());
        assertTrue(queue.getCurrentPosition().isValid());
    }

}
