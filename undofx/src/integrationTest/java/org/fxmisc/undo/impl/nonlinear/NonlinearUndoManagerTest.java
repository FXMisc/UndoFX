package org.fxmisc.undo.impl.nonlinear;
import com.nitorcreations.junit.runners.NestedRunner;
import org.fxmisc.richtextfx.DocumentDAG;
import org.fxmisc.richtextfx.DocumentModel;
import org.fxmisc.richtextfx.DocumentView;
import org.fxmisc.richtextfx.TextChange;
import org.fxmisc.richtextfx.UndoBubbleType;
import org.fxmisc.undo.NonlinearUndoManagerFactory;
import org.fxmisc.undo.UndoManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactfx.value.Var;

import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertNull;
import static org.fxmisc.undo.impl.nonlinear.FixedSizeNonlinearChangeQueue.BubbleForgetStrategy.OLDEST_INVALID_THEN_OLDEST_CHANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class NonlinearUndoManagerTest {

    private DocumentModel model;
    private DocumentDAG graph;

    @Before
    public void setup() {
        model = new DocumentModel();
        graph = new DocumentDAG(model);
    }

    @After
    public void cleanup() {
        graph.close();
    }

    public TextChange insertion() {
        return new TextChange(0, "", "a");
    }

    public TextChange insertion(String insertedText) {
        return new TextChange(0, "", insertedText);
    }

    public class ManagerTests {

        private DocumentView view;
        private UndoManager um;

        @Before
        public void setup() {
            view = new DocumentView(model);
        }

        private UndoManager newUndoManager(NonlinearChangeQueue<TextChange> queue, DocumentView view) {
            return new NonlinearUndoManager<>(queue, TextChange::invert, view::replace,
                    TextChange::mergeWith, view.changesDoneByThisViewEvents());
        }

        private void push(TextChange change) {
            view.replace(change);
        }

        @Test
        public void testUndoInvertsTheChange() {
            NonlinearChangeQueue<TextChange> queue = new UnlimitedNonlinearChangeQueue<>(graph);
            graph.addQueue(queue);
            Var<TextChange> lastAction = Var.newSimpleVar(null);
            um = new NonlinearUndoManager<>(queue, TextChange::invert,
                    c -> { lastAction.setValue(c); view.replace(c); },
                    TextChange::mergeWith, view.changesDoneByThisViewEvents());
            view.setUndoManager(um);

            TextChange first = insertion("aaa");
            TextChange second = insertion("bbb");

            view.replace(first);
            view.replace(second);
            assertNull(lastAction.getValue());

            um.undo();
            assertEquals(second.invert(), lastAction.getValue());

            um.undo();
            assertEquals(first.invert(), lastAction.getValue());

            um.redo();
            assertEquals(first, lastAction.getValue());

            um.redo();
            assertEquals(second.bumpPosition(first.getDifference()), lastAction.getValue());
        }

        @Test
        public void testMarkWithZeroSizeQueue() {
            NonlinearChangeQueue<TextChange> queue = new ZeroSizeNonlinearChangeQueue<>(graph);
            graph.addQueue(queue);
            um = newUndoManager(queue, view);
            view.setUndoManager(um);
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());

            push(insertion());
            um.mark();
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());
        }

        @Test
        public void testMarkWithFixedSizeQueue() {
            NonlinearChangeQueue<TextChange> queue = new FixedSizeNonlinearChangeQueue<>(4, graph,
                    OLDEST_INVALID_THEN_OLDEST_CHANGE, OLDEST_INVALID_THEN_OLDEST_CHANGE);
            graph.addQueue(queue);
            um = newUndoManager(queue, view);
            view.setUndoManager(um);
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());

            push(insertion());
            um.mark();
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());

            um.undo();
            um.undo();
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            push(insertion());
            push(insertion()); // overflow
            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());
        }

        @Test
        public void testMarkWithUnlimitedQueue() {
            NonlinearChangeQueue<TextChange> queue = new UnlimitedNonlinearChangeQueue<>(graph);
            graph.addQueue(queue);
            um = newUndoManager(queue, view);
            view.setUndoManager(um);

            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());

            push(insertion());
            um.mark();
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());

            um.undo();
            um.undo();
            assertTrue(um.atMarkedPositionProperty().get());

            push(insertion());
            push(insertion());
            push(insertion());
            push(insertion());
            assertFalse(um.atMarkedPositionProperty().get());
        }

        /**
         * Tests that isAtMarkedPosition() forces atMarkedPositionProperty()
         * become valid.
         */
        @Test
        public void testAtMarkedPositionRevalidation() {
            NonlinearChangeQueue<TextChange> queue = new ZeroSizeNonlinearChangeQueue<>(graph);
            graph.addQueue(queue);
            um = new NonlinearUndoManager<>(queue, TextChange::invert, view::replace,
                    TextChange::mergeWith, view.changesDoneByThisViewEvents());
            view.setUndoManager(um);

            um.atMarkedPositionProperty().get(); // atMarkedPositionProperty is now valid

            // we are going to expect two invalidations
            CountDownLatch latch = new CountDownLatch(2);
            um.atMarkedPositionProperty().addListener(observable -> latch.countDown());

            view.replace(insertion()); // atMarkedPositionProperty has been invalidated
            assertEquals(1, latch.getCount());

            um.isAtMarkedPosition(); // we want to test whether this caused revalidation of atMarkedPositionProperty

            view.replace(insertion()); // should have caused invalidation of atMarkedPositionProperty
            assertEquals(0, latch.getCount());
        }

        @Test(expected = IllegalStateException.class)
        public void testFailFastWhenExpectedChangeNotReceived() {
            NonlinearChangeQueue<TextChange> queue = new UnlimitedNonlinearChangeQueue<>(graph);
            graph.addQueue(queue);
            um = new NonlinearUndoManager<>(queue, TextChange::invert, c -> {},
                    TextChange::mergeWith, view.changesDoneByThisViewEvents());
            view.setUndoManager(um);
            view.replace(insertion());

            um.undo(); // should throw because the undone change is not received back
        }
    }

    public class LinearTests {

        private DocumentView view;
        private UndoManager um;

        public void setup(int capacity) {
            view = new DocumentView(model);
            NonlinearChangeQueue<TextChange> queue;
            if (capacity < 0 ) {
                queue = new UnlimitedNonlinearChangeQueue<>(graph);
                graph.addQueue(queue);
            } else if (capacity == 0) {
                queue = new ZeroSizeNonlinearChangeQueue<>(graph);
                // no need to add queue to graph
            } else {
                queue = new FixedSizeNonlinearChangeQueue<>(4, graph,
                        OLDEST_INVALID_THEN_OLDEST_CHANGE, OLDEST_INVALID_THEN_OLDEST_CHANGE);
                graph.addQueue(queue);
            }
            um = newUndoManager(queue, view);
            view.setUndoManager(um);
        }

        @After
        public void cleanup() {
            um.close();
        }

        private UndoManager newUndoManager(NonlinearChangeQueue<TextChange> queue, DocumentView view) {
            return new NonlinearUndoManager<>(queue, TextChange::invert, view::replace,
                    TextChange::mergeWith, view.changesDoneByThisViewEvents());
        }

        private void push(TextChange change) {
            view.replace(change);
        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        @Test
        public void testManagerWithZeroSizeQueue() {
            setup(0);

            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());

            String text1 = "aaa";
            String text2 = "b";

            TextChange tc1 = insertion(text1);
            TextChange tc2 = insertion(text2);

            push(tc1);
            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            // undo does nothing
            um.undo();
            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            // redo does nothing
            um.redo();
            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            push(tc2);
            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());

            // undo does nothing
            um.undo();
            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());

            // redo does nothing
            um.redo();
            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());
        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        @Test
        public void testManagerWithFixedSizeQueue() {
            setup(4);

            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());

            String text1 = "aaa";
            String text2 = "b";
            String text3 = "ccc";
            String text4 = "ddddddd";
            String text5 = "eeeeeeeeeeeee";

            TextChange tc1 = insertion(text1);
            TextChange tc2 = insertion(text2);
            TextChange tc3 = insertion(text3);
            TextChange tc4 = insertion(text4);
            TextChange tc5 = insertion(text5);

            push(tc1);
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            um.undo();
            assertFalse(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            assertEquals("", model.getText());

            um.redo();
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            push(tc2);
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());

            um.undo();
            assertTrue(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            um.redo();
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());

            push(tc3);
            push(tc4);
            push(tc5); // overflow: tc1 not in change queue

            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text5 + text4 + text3 + text2 + text1, model.getText());

            um.undo();
            um.undo();
            um.undo();
            um.undo();

            assertFalse(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            String text6 = "fff";
            push(insertion(text6));
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text6 + text1, model.getText());
        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        @Test
        public void testManagerWithUnlimitedQueue() {
            setup(-1);

            assertFalse(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());

            String text1 = "aaa";
            String text2 = "b";
            String text3 = "ccc";
            String text4 = "ddddddd";
            String text5 = "eeeeeeeeeeeee";

            TextChange tc1 = insertion(text1);
            TextChange tc2 = insertion(text2);
            TextChange tc3 = insertion(text3);
            TextChange tc4 = insertion(text4);
            TextChange tc5 = insertion(text5);

            push(tc1);
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            um.undo();
            assertFalse(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            assertEquals("", model.getText());

            um.redo();
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            push(tc2);
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());

            um.undo();
            assertTrue(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            um.redo();
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text2 + text1, model.getText());

            push(tc3);
            push(tc4);
            push(tc5);

            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text5 + text4 + text3 + text2 + text1, model.getText());

            um.undo();
            um.undo();
            um.undo();
            um.undo();

            assertTrue(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            assertEquals(text1, model.getText());

            um.undo();
            assertFalse(um.isUndoAvailable());
            assertTrue(um.isRedoAvailable());
            // TODO: By the looks of it, fails since tc1 does not get bumped by 1 [ tc2.getDifference() ]
            assertEquals("", model.getText());

            String text6 = "fff";
            push(insertion(text6));
            assertTrue(um.isUndoAvailable());
            assertFalse(um.isRedoAvailable());
            assertEquals(text6 + text1, model.getText());
        }

    }

    public class NonlinearTests {

        private DocumentView zView;
        private DocumentView fView;
        private DocumentView uView;

        private NonlinearUndoManagerFactory<TextChange, UndoBubbleType> factory = NonlinearUndoManagerFactory.factory(graph);

        public void setup() {
            zView = new DocumentView(model);
            installUndoManager(zView, 0);

            fView = new DocumentView(model);
            installUndoManager(fView, 10);

            uView = new DocumentView(model);
            installUndoManager(uView, -1);
        }

        private UndoManager installUndoManager(DocumentView view, int capacity) {
            return factory.create(capacity, view.changesDoneByThisViewEvents(), TextChange::invert, view::replace);
        }

        /**
         * Attempts to undo all changes that do not modify another before redoing them independent of which queue starts
         */
        public void testIndependentUndoRedo() {

        }

        /** An undoable change cannot be done when a later change has modified every aspect of it */
        public void testChangeNotUndoablePostOverridingChangePush() {

        }

        /** Attempts to undo a change after a later change has modified 1st one but then been undone */
        public void testChangeUndoablePostUndoOfOverridingChange() {

        }

        /** Attempt to undo a bubbly undo */
        public void testSimpleBubbleUndo() {

        }

        /** Attempt to redo a bubbly redo */
        public void testSimpleBubbleRedo() {

        }

        /** Attempt to undo a bubbly undo */
        public void testComplexBubbleUndo() {

        }

        /** Attempt to redo a bubbly redo */
        public void testComplexBubbleRedo() {

        }

    }

}
