package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.richtextfx.DocumentDAG;
import org.fxmisc.richtextfx.DocumentModel;
import org.fxmisc.richtextfx.DocumentView;
import org.fxmisc.richtextfx.TextChange;
import org.fxmisc.richtextfx.UndoBubbleType;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.nonlinear.NonlinearUndoManager;
import org.fxmisc.undo.impl.nonlinear.UnlimitedNonlinearChangeQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class UnlimitedNonlinearChangeQueueTest {

    private DocumentModel model;
    private DocumentDAG graph;
    private UnlimitedNonlinearChangeQueue<TextChange, UndoBubbleType> queue;
    private DocumentView view;

    @Before
    public void setup() {
        model = new DocumentModel();
        view = new DocumentView(model);

        graph = new DocumentDAG(model);
        queue = new UnlimitedNonlinearChangeQueue<>(graph);
        graph.addQueue(queue);

        UndoManager um = new NonlinearUndoManager<>(queue, TextChange::invert, view::replace,
                TextChange::mergeWith, view.changesDoneByThisViewEvents());
        view.setUndoManager(um);
    }

    @After
    public void cleanup() {
        queue.close();
        graph.close();
    }

    private void push(TextChange... changes) {
        for (TextChange c : changes) {
            view.replace(c);
        }
    }

    private void undo() {
        view.undo();
    }

    private void redo() {
        view.redo();
    }

    private TextChange insertion() {
        return insertion("bbb");
    }

    private TextChange insertion(String insertionText) {
        return new TextChange(0, "", insertionText);
    }

    @Test
    public void testPositionValidityOnUndo() {
        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();

        push(insertion("aaa"));
        ChangeQueue.QueuePosition pos1 = queue.getCurrentPosition();
        push(insertion("bbb"));
        ChangeQueue.QueuePosition pos2 = queue.getCurrentPosition();
        push(insertion("ccc"));
        ChangeQueue.QueuePosition pos3 = queue.getCurrentPosition();
        push(insertion("ddd"));
        ChangeQueue.QueuePosition pos4 = queue.getCurrentPosition();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());

        undo();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());

        // overwrite redo (4) with new change that equals old one (4)
        push(insertion("ddd"));

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        // pos4 is still true because new change equals old one
        assertTrue(pos4.isValid());

        // overwrite redo (4) with new different change (5)
        undo();
        push(insertion("eee"));

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        // now no longer valid
        assertFalse(pos4.isValid());

        undo();
        undo();
        undo();
        undo();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        // still not valid
        assertFalse(pos4.isValid());

        push(insertion("fff"));

        assertTrue(pos0.isValid());
        assertFalse(pos1.isValid());
        assertFalse(pos2.isValid());
        assertFalse(pos3.isValid());
        assertFalse(pos4.isValid());
    }

    @Test
    public void testPositionValidityOnForgetHistory() {
        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();

        push(insertion());
        ChangeQueue.QueuePosition pos1 = queue.getCurrentPosition();
        push(insertion());
        ChangeQueue.QueuePosition pos2 = queue.getCurrentPosition();
        push(insertion());
        ChangeQueue.QueuePosition pos3 = queue.getCurrentPosition();
        push(insertion());
        ChangeQueue.QueuePosition pos4 = queue.getCurrentPosition();

        undo();
        undo();
        queue.forgetHistory();

        assertFalse(pos0.isValid());
        assertFalse(pos1.isValid());
        assertFalse(pos2.isValid());
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());
    }

    @Test
    public void testPositionEquality() {
        push(insertion());
        ChangeQueue.QueuePosition pos = queue.getCurrentPosition();
        assertEquals(pos, queue.getCurrentPosition());
        push(insertion());
        assertNotEquals(pos, queue.getCurrentPosition());
        undo();
        assertEquals(pos, queue.getCurrentPosition());
        undo();
        assertEquals(pos, queue.getCurrentPosition());
        redo();
        assertEquals(pos, queue.getCurrentPosition());
        undo();
        push(insertion());
        assertNotEquals(pos, queue.getCurrentPosition());
    }

}