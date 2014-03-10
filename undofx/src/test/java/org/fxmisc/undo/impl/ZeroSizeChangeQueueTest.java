package org.fxmisc.undo.impl;

import static org.junit.Assert.*;

import org.fxmisc.undo.impl.ChangeQueue.QueuePosition;
import org.junit.Test;

public class ZeroSizeChangeQueueTest {

    @Test
    public void testPositionValidityOnOverflow() {
        ChangeQueue<Integer> queue = new ZeroSizeChangeQueue<>();
        QueuePosition pos0 = queue.getCurrentPosition();
        assertTrue(pos0.isValid());
        queue.push(1);
        assertFalse(pos0.isValid());
        assertTrue(queue.getCurrentPosition().isValid());
    }

}
