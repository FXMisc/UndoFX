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

import static org.fxmisc.undo.impl.nonlinear.FixedSizeNonlinearChangeQueue.BubbleStrategy.FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE;

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

    public class LinearTests {

        private DocumentView view;
        private UndoManager undoManager;

        @Before
        public void setup() {
            view = new DocumentView(model);
        }

        @After
        public void cleanup() {
            undoManager.close();
        }

        private UndoManager newUndoManager(NonlinearChangeQueue<TextChange> queue, DocumentView view) {
            return new NonlinearUndoManager<>(queue, TextChange::invert, view::replace,
                    TextChange::mergeWith, view.changesDoneByThisViewEvents());
        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        public class SingleZeroSizeQueue {

            private ZeroSizeNonlinearChangeQueue<TextChange, UndoBubbleType> queue;

            @Before
            public void setup() {
                queue = new ZeroSizeNonlinearChangeQueue<>(graph);
                graph.addQueue(queue);
                undoManager = newUndoManager(queue, view);
                view.setUndoManager(undoManager);
            }

            @Test
            public void test() {

            }

        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        public class SingleFixedSizeQueue {

            private FixedSizeNonlinearChangeQueue<TextChange, UndoBubbleType> queue;

            @Before
            public void setup() {
                queue = new FixedSizeNonlinearChangeQueue<>(10, graph, FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE, FORGET_OLDEST_INVALID_THEN_OLDEST_CHANGE);
                graph.addQueue(queue);
                undoManager = newUndoManager(queue, view);
                view.setUndoManager(undoManager);
            }

            @Test
            public void test() {

            }

        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        public class SingleUnlimitedSizeQueue {

            private UnlimitedNonlinearChangeQueue<TextChange, UndoBubbleType> queue;

            @Before
            public void setup() {
                queue = new UnlimitedNonlinearChangeQueue<>(graph);
                graph.addQueue(queue);
                undoManager = newUndoManager(queue, view);
                view.setUndoManager(undoManager);
            }

            @Test
            public void test() {

            }

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
