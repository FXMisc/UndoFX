package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.richtextfx.DocumentDAG;
import org.fxmisc.richtextfx.DocumentModel;
import org.fxmisc.richtextfx.TextChange;
import org.fxmisc.richtextfx.UndoBubbleType;
import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.nonlinear.ZeroSizeNonlinearChangeQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZeroSizeNonlinearChangeQueueTest {

    private DocumentDAG graph;
    private ZeroSizeNonlinearChangeQueue<TextChange, UndoBubbleType> queue;

    @Before
    public void setup() {
        graph = new DocumentDAG(new DocumentModel());
        queue = new ZeroSizeNonlinearChangeQueue<>(graph);
    }

    @After
    public void cleanup() {
        graph.close();
    }

    @Test
    public void testPositionValidityOnOverflow() {
        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();
        assertTrue(pos0.isValid());
        queue.push(new TextChange(0, "", "aaa"));
        assertFalse(pos0.isValid());
        assertTrue(queue.getCurrentPosition().isValid());
    }

}
