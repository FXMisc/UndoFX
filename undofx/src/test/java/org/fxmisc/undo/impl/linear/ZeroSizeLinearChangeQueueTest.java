package org.fxmisc.undo.impl.linear;

import static org.junit.Assert.*;

import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.ChangeQueue.QueuePosition;
import org.fxmisc.undo.impl.linear.ZeroSizeLinearChangeQueue;
import org.junit.Test;

public class ZeroSizeLinearChangeQueueTest {

    @Test
    public void testPositionValidityOnOverflow() {
        LinearChangeQueue<Integer> queue = new ZeroSizeLinearChangeQueue<>();
        QueuePosition pos0 = queue.getCurrentPosition();
        assertTrue(pos0.isValid());
        queue.push(1);
        assertFalse(pos0.isValid());
        assertTrue(queue.getCurrentPosition().isValid());
    }

}
