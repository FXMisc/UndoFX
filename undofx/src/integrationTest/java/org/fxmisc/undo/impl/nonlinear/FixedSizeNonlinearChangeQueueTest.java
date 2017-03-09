package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.richtextfx.DocumentDAG;
import org.fxmisc.richtextfx.DocumentModel;
import org.fxmisc.richtextfx.DocumentView;
import org.fxmisc.richtextfx.TextChange;
import org.fxmisc.richtextfx.UndoBubbleType;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.impl.ChangeQueue;
import org.fxmisc.undo.impl.nonlinear.FixedSizeNonlinearChangeQueue;
import org.fxmisc.undo.impl.nonlinear.NonlinearUndoManager;

import org.junit.After;
import org.junit.Test;

import static org.fxmisc.undo.impl.nonlinear.FixedSizeNonlinearChangeQueue.BubbleStrategy.FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class FixedSizeNonlinearChangeQueueTest {

    private DocumentModel model;
    private DocumentDAG graph;
    private FixedSizeNonlinearChangeQueue<TextChange, UndoBubbleType> queue;
    private DocumentView view;

    public void setup(int capacity) {
        model = new DocumentModel();
        view = new DocumentView(model);

        graph = new DocumentDAG(model);
        queue = new FixedSizeNonlinearChangeQueue<>(capacity, graph, FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE, FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE);
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

    private TextChange insertion(String insertionText) {
        return new TextChange(0, "", insertionText);
    }

    @Test
    public void testOverflow() {
        setup(5);

        push(insertion("a"));
        push(insertion("b"));
        push(insertion("c"));
        push(insertion("d"));
        push(insertion("e"));
        push(insertion("f"));

        assertFalse(queue.hasNext());
        assertTrue(queue.hasPrev());

        undo(); // undos 'f'
        assertEquals("edcba", model.getText());
        undo(); // undos 'e'
        assertEquals("dcba", model.getText());
        undo(); // undos 'd'
        assertEquals("cba", model.getText());
        undo(); // undos 'c'
        assertEquals("ba", model.getText());
        undo(); // undos 'b'

        assertFalse(queue.hasPrev());
        assertTrue(queue.hasNext());
    }

    @Test
    public void testPositionValidityOnOverflow() {
        // create empty queue
        setup(1);

        // check that the initial position is valid
        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();
        assertTrue(pos0.isValid());

        // push first element
        push(insertion("a"));

        // check that the initial position is still valid
        // and that the current position is valid as well
        assertTrue(pos0.isValid());
        ChangeQueue.QueuePosition pos1 = queue.getCurrentPosition();
        assertTrue(pos1.isValid());

        // push one more element
        push(insertion("b"));

        // check that initial position is now invalid,
        // previous position is still valid and
        // the current position is valid as well
        assertFalse(pos0.isValid());
        assertTrue(pos1.isValid());
        ChangeQueue.QueuePosition pos2 = queue.getCurrentPosition();
        assertTrue(pos2.isValid());

        // push two elements at once
        push(insertion("c"));
        push(insertion("d"));

        // check that all previous positions are invalid
        assertFalse(pos0.isValid());
        assertFalse(pos1.isValid());
        assertFalse(pos2.isValid());
    }


    @Test
    public void testPositionValidityOnOverflowWithMiddlePosition() {
        // create empty queue
        setup(3);

        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();

        push(insertion("a"));
        ChangeQueue.QueuePosition pos1 = queue.getCurrentPosition();

        push(insertion("b"));
        ChangeQueue.QueuePosition pos2 = queue.getCurrentPosition();

        push(insertion("c"));
        ChangeQueue.QueuePosition pos3 = queue.getCurrentPosition();

        // list is now full
        assertTrue(pos0.isValid()); // "empty" position
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());

        // now overflow the list by 2
        push(insertion("d"));
        ChangeQueue.QueuePosition pos4 = queue.getCurrentPosition();

        push(insertion("e"));
        ChangeQueue.QueuePosition pos5 = queue.getCurrentPosition();

        assertFalse(pos0.isValid());
        assertFalse(pos1.isValid());
        assertTrue(pos2.isValid()); // empty position
        assertTrue(pos3.isValid());
        assertTrue(pos4.isValid());
        assertTrue(pos5.isValid());
    }

    @Test
    public void testPositionValidityOnUndo() {
        setup(4);

        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();
        push(insertion("a"));
        ChangeQueue.QueuePosition pos1 = queue.getCurrentPosition();
        push(insertion("b"));
        ChangeQueue.QueuePosition pos2 = queue.getCurrentPosition();
        push(insertion("c"));
        ChangeQueue.QueuePosition pos3 = queue.getCurrentPosition();
        push(insertion("d"));
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

        push(insertion("e"));


        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertFalse(pos4.isValid());

        undo();
        undo();
        undo();
        undo();

        assertTrue(pos0.isValid());
        assertTrue(pos1.isValid());
        assertTrue(pos2.isValid());
        assertTrue(pos3.isValid());
        assertFalse(pos4.isValid());

        push(insertion("e"));

        assertTrue(pos0.isValid());
        assertFalse(pos1.isValid());
        assertFalse(pos2.isValid());
        assertFalse(pos3.isValid());
        assertFalse(pos4.isValid());
    }

    @Test
    public void testPositionValidityOnForgetHistory() {
        setup(4);

        ChangeQueue.QueuePosition pos0 = queue.getCurrentPosition();
        push(insertion("a"));
        ChangeQueue.QueuePosition pos1 = queue.getCurrentPosition();
        push(insertion("b"));
        ChangeQueue.QueuePosition pos2 = queue.getCurrentPosition();
        push(insertion("c"));
        ChangeQueue.QueuePosition pos3 = queue.getCurrentPosition();
        push(insertion("d"));
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
        setup(2);

        push(insertion("a"));
        ChangeQueue.QueuePosition pos = queue.getCurrentPosition();
        assertEquals(pos, queue.getCurrentPosition());

        push(insertion("b"));
        assertNotEquals(pos, queue.getCurrentPosition());

        undo();
        assertEquals(pos, queue.getCurrentPosition());

        undo();
        assertNotEquals(pos, queue.getCurrentPosition());

        redo();
        assertEquals(pos, queue.getCurrentPosition());

        undo();
        push(insertion("c"));
        assertNotEquals(pos, queue.getCurrentPosition());
    }
}
