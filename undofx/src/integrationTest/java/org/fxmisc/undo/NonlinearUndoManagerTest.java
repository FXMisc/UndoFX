package org.fxmisc.undo;
import com.nitorcreations.junit.runners.NestedRunner;
import org.junit.runner.RunWith;

@RunWith(NestedRunner.class)
public class NonlinearUndoManagerTest {

    public class LinearTests {

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        public class SingleZeroSizeQueue {

        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        public class SingleFixedSizeQueue {

        }

        /** Tests nonlinear undo/redo using only one zero-sized queue */
        public class SingleUnlimitedSizeQueue {

        }

    }

    public class NonlinearTests {

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
