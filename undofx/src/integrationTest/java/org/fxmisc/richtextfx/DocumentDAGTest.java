package org.fxmisc.richtextfx;

import com.nitorcreations.junit.runners.NestedRunner;
import org.fxmisc.undo.impl.nonlinear.BubbledRedoResult;
import org.fxmisc.undo.impl.nonlinear.BubbledUndoResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.fail;

@RunWith(NestedRunner.class)
public class DocumentDAGTest {

    void FOR(int times, Runnable DO) {
        for (int i = 0; i < times; i++) { DO.run(); }
    }

    /** Shortcut for new TextChange(start, "", "b" * insertedLength) */
    TextChange insertion(int start, int insertedLength) {
        return replacement(start, 0, insertedLength);
    }

    /** Shortcut for new TextChange(start, "a" * removedLength, "") */
    TextChange deletion(int start, int removedLength) {
        return replacement(start, removedLength, 0);
    }

    /** Shortcut for new TextChange(start, "a" * removedLength, "b" * insertedLength) */
    TextChange replacement(int start, int removedLength, int insertedLength) {
        return replacement(start, removedLength, insertedLength, "a", "b");
    }

    /** Shortcut for new TextChange(start, "a" * removedLength, "b" * insertedLength) */
    TextChange replacement(int start, int removedLength, int insertedLength, String removedChar, String insertedChar) {
        StringBuilder remSB = new StringBuilder();
        FOR(removedLength, () -> remSB.append(removedChar));

        StringBuilder insSB = new StringBuilder();
        FOR(insertedLength, () -> insSB.append(insertedChar));

        return new TextChange(start, remSB.toString(), insSB.toString());
    }

    /** Shortcut for new TextChange(start, deleted, inserted) */
    TextChange tc(int start, String deleted, String inserted) {
        return new TextChange(start, deleted, inserted);
    }

    /** Shortcut for new TextChange(start, deletedText, "") */
    TextChange deletion(int start, String deletedText) {
        return new TextChange(start, deletedText, "");
    }

    /** Shortcut for new TextChange(start, "", insertedText) */
    TextChange insertion(int start, String insertedText) {
        return new TextChange(start, "", insertedText);
    }

    List<TextChange> list(TextChange... changes) {
        return Arrays.asList(changes);
    }

    DocumentModel model = new DocumentModel(false);
    DocumentDAG graph = new DocumentDAG(model);

    public class FirstIsModifiedBySecondTests {

        private TextChange original = replacement(10, 10, 10);

        public class WhenChangeOccursInFrontOfOriginal {

            @Test
            public void deletionDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, deletion(0, "aaa"));
            }

            @Test
            public void replacementDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, replacement(0, 3, 3));
                assert !graph.firstIsModifiedBySecond(original, replacement(0, 2, 3));
                assert !graph.firstIsModifiedBySecond(original, replacement(0, 3, 2));
            }

            @Test
            public void insertionDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, insertion(0, "aaa"));
            }
        }

        public class WhenDeletingSomeOrAllOfOriginal {

            @Test
            public void deletingStartModifiesIt() {
                assert graph.firstIsModifiedBySecond(original, deletion(8, 5));

                assert graph.firstIsModifiedBySecond(original, replacement(8, 5, 6));
                assert graph.firstIsModifiedBySecond(original, replacement(8, 6, 5));
                assert graph.firstIsModifiedBySecond(original, replacement(8, 5, 5));
            }

            @Test
            public void deletingAllModifiesIt() {
                assert graph.firstIsModifiedBySecond(original, deletion(10, 10));

                assert graph.firstIsModifiedBySecond(original, replacement(10, 10, 12));
                assert graph.firstIsModifiedBySecond(original, replacement(10, 10, 5));
                assert graph.firstIsModifiedBySecond(original, replacement(10, 10, 10, "c", "d"));
            }

            @Test
            public void deletingMiddleModifiesIt() {
                assert graph.firstIsModifiedBySecond(original, deletion(14, 4));

                assert graph.firstIsModifiedBySecond(original, replacement(11, 5, 6));
                assert graph.firstIsModifiedBySecond(original, replacement(11, 6, 5));
                assert graph.firstIsModifiedBySecond(original, replacement(11, 5, 5));
            }

            @Test
            public void deletingEndModifiesIt() {
                assert graph.firstIsModifiedBySecond(original, deletion(18, 5));

                assert graph.firstIsModifiedBySecond(original, replacement(18, 5, 6));
                assert graph.firstIsModifiedBySecond(original, replacement(18, 6, 5));
                assert graph.firstIsModifiedBySecond(original, replacement(18, 5, 5));
            }

        }

        public class WhenChangeOccursAfterOriginal {

            @Test
            public void deletionDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, deletion(30, 10));
            }

            @Test
            public void replacementDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, replacement(30, 10, 8));
                assert !graph.firstIsModifiedBySecond(original, replacement(30, 10, 10));
                assert !graph.firstIsModifiedBySecond(original, replacement(30, 8, 10));
            }

            @Test
            public void insertionDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, insertion(30, 10));
            }

        }

        public class WhenHaveMiscellaneousSituations {

            @Test
            public void changeDoesNotModifyItself() {
                assert !graph.firstIsModifiedBySecond(original, original);
            }

            @Test
            public void endsWhereOriginalStartsDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, deletion(5, 5));
                assert !graph.firstIsModifiedBySecond(original, insertion(5, 5));

                assert !graph.firstIsModifiedBySecond(original, replacement(5, 5, 6));
                assert !graph.firstIsModifiedBySecond(original, replacement(5, 5, 3));
                assert !graph.firstIsModifiedBySecond(original, replacement(5, 5, 5));
            }

            @Test
            public void startsWhereOriginalEndsDoesNotModifyIt() {
                assert !graph.firstIsModifiedBySecond(original, deletion(20, 5));
                assert !graph.firstIsModifiedBySecond(original, insertion(20, 5));

                assert !graph.firstIsModifiedBySecond(original, replacement(20, 5, 5));
                assert !graph.firstIsModifiedBySecond(original, replacement(20, 5, 3));
                assert !graph.firstIsModifiedBySecond(original, replacement(20, 3, 5));
            }

        }

    }

    public class UpdateUndoTests {

        private int start = 10;
        TextChange outdatedUndo = replacement(start, 10, 10);

        private int bumpAmount = 3;
        private int changeLength = 4;

        public class WhenChangeOccursBefore {

            @Test
            public void insertingSameLengthAsDeletionDoesNotUpdate() {
                assert graph.updateUndo(outdatedUndo, replacement(0, 4, 4)).getStart() == start;
            }

            @Test
            public void insertionUpdatesUndo() {
                TextChange addsLetters = replacement(0, changeLength, changeLength + bumpAmount);
                assert graph.updateUndo(outdatedUndo, addsLetters).getStart() == start + bumpAmount;
            }

            @Test
            public void deletionUpdatesUndo() {
                TextChange deletesLetters = replacement(0, changeLength + bumpAmount, changeLength);
                assert graph.updateUndo(outdatedUndo, deletesLetters).getStart() == start - bumpAmount;
            }
        }

        public class WhenChangeOccursAfter {

            @Test
            public void doesNotUpdate() {
                TextChange occursAfterwards = deletion(30, 10);
                assert graph.updateUndo(outdatedUndo, occursAfterwards).getStart() == start;
            }

        }

        public class WhenChangeOccursWithin {

            @Test
            public void doesNotUpdate() {
                TextChange occursWithinUndo = replacement(start + 2, 5, 4);
                assert graph.updateUndo(outdatedUndo, occursWithinUndo).getStart() == start;
            }

        }

    }

    public class IsUndoValidTests {

        @Test
        public void immediatelyDoneChangeIsValid() {
            TextChange insertion = insertion(0, 5);
            model.replace(insertion);

            assert graph.isUndoValid(insertion);
        }

        @Test
        public void bubblyLeftChangeIsValid() {
            TextChange insertion = insertion(0, 10);
            model.replace(insertion);
            model.replace(4, 8, "zzz");

            assert graph.isUndoValid(insertion);
        }

        @Test
        public void bubblyRightChangeIsValid() {
            TextChange insertion = insertion(0, 10);
            model.replace(insertion);
            model.replace(0, 4, "zzz");

            assert graph.isUndoValid(insertion);
        }

        @Test
        public void bubblyMiddleChangeIsValid() {
            TextChange insertion = insertion(0, 10);
            model.replace(insertion);
            model.replace(0, 2, "zzz");
            model.replace(8, 10, "yyy");

            assert graph.isUndoValid(insertion);
        }

        @Test
        public void changeWithNoSimilarityIsInvalid() {
            TextChange insertion = insertion(0, 10);
            model.replace(insertion);
            model.replaceText("zzzzzzzzzzz");

            assert !graph.isUndoValid(insertion);
        }

        @Test
        public void deletionChangeIsValid() {
            assert graph.isUndoValid(deletion(0, 5));
        }

    }

    public class DoBubbleUndoTests {

        private final String aText = "aaaaa";
        private final String bText = "bbbbb";
        private final String cText = "ccccc";
        private final String dText = "ddddd";
        private final String removalText = "eee";

        private BubbledUndoResult<TextChange, UndoBubbleType> result;
        private TextChange bubbled;
        private TextChange grounded;

        public class ThrowExceptionWhen {

            @Test
            public void bubblyUndoHasZeroDependencies() {
                List<TextChange> noDependencies = Collections.emptyList();
                TextChange noChange = insertion(0, "insert");
                try {
                    graph.doBubbleUndo(noChange, noDependencies);
                    fail();
                } catch (IllegalStateException e) {
                    // dependencies can't be empty
                }
            }

            @Test
            public void foundZeroValidUndoRanges() {
                TextChange change = replacement(10, 5, 5);
                try {
                    graph.doBubbleUndo(change, list(change));
                    fail();
                } catch (IllegalStateException e) {
                    // there must be at least one valid undo range
                }
            }

            @Test
            public void undoRangeDoesNotMatchAnyPortionOfModelTest() {
                TextChange bubblyRight = new TextChange(0, removalText, aText + aText);
                TextChange dependency = new TextChange(0, aText, bText);
                // make model have text that does not match
                model.replaceText("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
                try {
                    graph.doBubbleUndo(bubblyRight, list(dependency));
                    fail();
                } catch (IllegalStateException e) {
                    // a portion of undo's insertion must equal the corresponding range in the model's text
                }
            }

        }

        @Test
        public void bubblyRightUndoWillBubble() {
            // simulate changes so that have grounded left, bubbly right
            // bubblyRight = "aaaaa" + "ccccc"
            // dependency =  "bbbbb"
            TextChange bubblyRight = new TextChange(0, removalText, aText + cText);
            TextChange dependency = new TextChange(0, aText, bText);

            // simulate changes so that have bubbly right, grounded left
            model.replace(bubblyRight);
            model.replace(dependency);
            assert model.getText().equals(bText + cText);
            result = graph.doBubbleUndo(bubblyRight, list(dependency));

            grounded = result.getGrounded();
            assert grounded.getStart() == bubblyRight.getStart();
            assert grounded.getRemoved().equals("");
            assert grounded.getInserted().equals(aText);

            bubbled = result.getBubbled();
            assert bubbled.getStart() == aText.length();
            assert bubbled.getRemoved().equals(bubblyRight.getRemoved());
            assert bubbled.getInserted().equals(cText);
        }

        @Test
        public void bubblyLeftUndoWillBubble() {
            // simulate changes so that have bubbly left, grounded right
            // bubblyLeft = "aaaaa" + "ccccc"
            // dependency =           "bbbbb"
            String b = "b";
            TextChange bubblyLeft = new TextChange(0, removalText, aText + cText);
            TextChange dependency = new TextChange(aText.length(), cText, b);

            model.replace(bubblyLeft);
            model.replace(dependency);
            BubbledUndoResult<TextChange, UndoBubbleType> result = graph.doBubbleUndo(bubblyLeft, list(dependency));

            TextChange bubbled = result.getBubbled();
            assert bubbled.getStart() == bubblyLeft.getStart();
            assert bubbled.getRemoved().equals(bubblyLeft.getRemoved());
            assert bubbled.getInserted().equals(aText);

            grounded = result.getGrounded();
            assert grounded.getStart() == bubblyLeft.getStart();
            assert grounded.getRemoved().equals("");
            assert grounded.getInserted().equals(cText);
        }

        @Test
        public void bubblyMiddleUndoWillBubble() {
            // simulate changes so that have bubbly middle, grounded left + right
            // bubblyRight =     "aaaaa" + "ccccc" + "ddddd"
            // leftDependency =  "bbbbb"
            // rightDependency =                     "bbbbb"
            TextChange bubblyMiddle = new TextChange(0, removalText, aText + cText + dText);
            TextChange leftDependency = new TextChange(0, aText, bText);
            TextChange rightDependency = new TextChange(aText.length() + cText.length(), dText, bText);

            model.replace(bubblyMiddle);
            model.replace(leftDependency);
            model.replace(rightDependency);
            result = graph.doBubbleUndo(bubblyMiddle, list(leftDependency, rightDependency));

            bubbled = result.getBubbled();
            assert bubbled.getStart() == aText.length();
            assert bubbled.getRemoved().equals(bubblyMiddle.getRemoved());
            assert bubbled.getInserted().equals(cText);

            grounded = result.getGrounded();
            assert grounded.getStart() == bubblyMiddle.getStart();
            assert grounded.getRemoved().equals("");
            assert grounded.getInserted().equals(aText + dText);
        }
    }

    public class UpdateBubbledUndoDependencyTests {

        public class ExpectNoPositionBumpWhen {

            @Test
            public void dependencyModifiesGroundedLeftWithRightBubble() {
                String groundedText = "aaa";
                String bubblyText = "bbb";

                TextChange bubblyRightUndo = insertion(0, groundedText + bubblyText);
                TextChange modifiesGroundedLeft = deletion(0, groundedText);

                model.replace(bubblyRightUndo);
                model.replace(modifiesGroundedLeft);
                assert model.getText().equals(bubblyText);

                assert graph.isUndoValid(bubblyRightUndo);

                BubbledUndoResult<TextChange, UndoBubbleType> result = graph.doBubbleUndo(bubblyRightUndo, list(modifiesGroundedLeft));

                TextChange updatedDependency = graph.updateBubbledUndoDependency(modifiesGroundedLeft, bubblyRightUndo, result);

                assert updatedDependency.equals(modifiesGroundedLeft);
            }

            @Test
            public void dependencyModifiesGroundedLeftWithMiddleBubble() {
                String left = "aaa";
                String middle = "bbb";
                String right = "ccc";

                TextChange bubblyMiddleUndo = insertion(0, left + middle + right);
                TextChange modifiesGroundedLeft = deletion(0 , left);
                TextChange modifiesGroundedRight = deletion(left.length() + middle.length(), right);

                model.replace(bubblyMiddleUndo);
                model.delete(0, left.length());
                model.delete(middle.length(), middle.length() + right.length());

                assert graph.isUndoValid(bubblyMiddleUndo);

                BubbledUndoResult<TextChange, UndoBubbleType> result = graph.doBubbleUndo(bubblyMiddleUndo, list(modifiesGroundedLeft, modifiesGroundedRight));

                TextChange updatedDependency = graph.updateBubbledUndoDependency(modifiesGroundedLeft, bubblyMiddleUndo, result);

                assert updatedDependency.equals(modifiesGroundedLeft);
            }

        }

        public class ExpectPositionBumpWhen {

            @Test
            public void dependencyModifiesGroundedRight() {
                String bubblyText = "bbb";
                String groundedText = "aaa";
                TextChange bubblyLeftUndo = insertion(0, bubblyText + groundedText);
                TextChange deletesGroundedRight = deletion(bubblyText.length(), groundedText);

                model.replace(bubblyLeftUndo);
                model.replace(deletesGroundedRight);

                assert graph.isUndoValid(bubblyLeftUndo);

                BubbledUndoResult<TextChange, UndoBubbleType> result = graph.doBubbleUndo(bubblyLeftUndo, list(deletesGroundedRight));

                TextChange updatedDependency = graph.updateBubbledUndoDependency(deletesGroundedRight, bubblyLeftUndo, result);

                assert updatedDependency.equals(deletesGroundedRight.bumpPosition(-bubblyText.length()));
            }

            @Test
            public void dependencyModifiesGroundedRightWithMiddleBubble() {
                // bubblyMiddle = "aaa" + "bbb" + "ccc"
                // groundedLeft = "aaa"
                // groundedR    =                 "ccc"
                // modelText    =         "bbb"
                String left = "aaa";
                String middle = "bbb";
                String right = "ccc";

                TextChange bubblyMiddleUndo = insertion(0, left + middle + right);
                TextChange deletesGroundedLeft = deletion(0, left);
                TextChange deletesGroundedRight = deletion(left.length() + middle.length(), right);

                model.replace(bubblyMiddleUndo);
                model.delete(0, left.length());
                model.delete(middle.length(), middle.length() + right.length());

                assert model.getText().equals(middle);

                BubbledUndoResult<TextChange, UndoBubbleType> result = graph.doBubbleUndo(bubblyMiddleUndo, list(deletesGroundedLeft, deletesGroundedRight));

                TextChange updatedDependency = graph.updateBubbledUndoDependency(deletesGroundedRight, bubblyMiddleUndo, result);

                assert updatedDependency.equals(deletesGroundedRight.bumpPosition(-middle.length()));
            }

        }

    }

    public class UpdateDependencyTests {

        private int dependencyStart = 10;

        private TextChange dependency = replacement(dependencyStart, 3, 4, "b", "c");

        public class WhenSomethingIsDeletedLeftOfDependencysDependency {

            @Test
            public void dependencyGetsBumped() {
                int bubbleTextLength = 5;

                TextChange oldChange = replacement(8, 4, 8);
                TextChange newChange = oldChange.bumpPosition(-bubbleTextLength);

                assert graph.updateDependency(dependency, oldChange, newChange).getStart() == dependencyStart - bubbleTextLength;
            }

        }

        public class WhenSomethingIsBubbledRightOfDependencysDependency {

            @Test
            public void dependencyIsNotBumped() {
                TextChange change = replacement(8, 4, 8);

                assert graph.updateDependency(dependency, change, change).getStart() == dependencyStart;
            }

        }

    }

    public class UpdateRedoTests {

        private final int start = 10;
        private final TextChange outdatedRedo = replacement(start, 10, 10);

        private final int bumpAmount = 3;

        public class WhenChangeOccursBeforeRedo {

            @Test
            public void insertingSameLengthAsDeletionDoesNotUpdateRedo() {
                assert graph.updateRedo(outdatedRedo, replacement(0, 4, 4)).getStart() == start;
            }

            @Test
            public void insertionUpdatesRedo() {
                assert graph.updateRedo(outdatedRedo, insertion(0, bumpAmount)).getStart() == start + bumpAmount;
            }

            @Test
            public void deletionUpdatesRedo() {
                assert graph.updateRedo(outdatedRedo, deletion(0, bumpAmount)).getStart() == start - bumpAmount;
            }

        }

        public class WhenChangeOccursAfterRedo {

            @Test
            public void redoIsNotUpdated() {
                assert graph.updateRedo(outdatedRedo, insertion(30, 4)).getStart() == start;
                assert graph.updateRedo(outdatedRedo, deletion(30, 4)).getStart() == start;

                assert graph.updateRedo(outdatedRedo, replacement(30, 5, 4)).getStart() == start;
                assert graph.updateRedo(outdatedRedo, replacement(30, 4, 5)).getStart() == start;
                assert graph.updateRedo(outdatedRedo, replacement(30, 5, 5)).getStart() == start;
            }

        }

        public class WhenChangeOccursWithinRedo {

            @Test
            public void redoIsNotUpdated() {
                assert graph.updateRedo(outdatedRedo, insertion(start + 2, 4)).getStart() == start;
                assert graph.updateRedo(outdatedRedo, deletion(start + 2, 4)).getStart() == start;

                assert graph.updateRedo(outdatedRedo, replacement(start + 2, 5, 4)).getStart() == start;
                assert graph.updateRedo(outdatedRedo, replacement(start + 2, 4, 5)).getStart() == start;
                assert graph.updateRedo(outdatedRedo, replacement(start + 2, 5, 5)).getStart() == start;
            }

        }
    }

    public class IsRedoValidTests {

        @Test
        public void redoThatInsertsTextIsAlwaysValid() {
            // removes nothing
            assert graph.isRedoValid(insertion(0, "hello"));

            // removes something
            assert graph.isRedoValid(tc(0, "removed", "inserted"));
        }

        @Test
        public void redoThatCompletelyMatchesModelTextIsValid() {
            String text = "zzzzzzzzzzz";
            model.replaceText(text);

            TextChange redo = deletion(0, text);

            assert graph.isRedoValid(redo);
        }

        @Test
        public void redoThatPartiallyMatchesModelTextIsValid() {
            model.replaceText("aza");

            TextChange redo = deletion(0, "zzz");
            assert graph.isRedoValid(redo);
        }

        @Test
        public void redoWithNoSimilarityToModelTextIsInvalid() {
            model.replaceText("zzzzzzzzzz");
            TextChange redo = deletion(0, "aaa");
            assert !graph.isRedoValid(redo);
        }
    }

    public class BubbleRedoTests {

        @Test
        public void redoThatRemovesNothingIsNotBubbled() {
            TextChange redo = insertion(0, "some insertion");

            assert graph.isRedoValid(redo);
            BubbledRedoResult<TextChange> result = graph.bubbleRedo(redo);

            assert !result.wasBubbled();
        }

        @Test
        public void redoThatDoesNotNeedBubbleIsNotBubbled() {
            String initialText = "aaa";
            model.replaceText(initialText);

            TextChange redo = tc(0, initialText, "some insertion");

            assert graph.isRedoValid(redo);
            BubbledRedoResult<TextChange> result = graph.bubbleRedo(redo);

            assert !result.wasBubbled();
        }

        @Test
        public void redoThatInsertsSomethingButCantDeleteAnythingIsBubbled() {
            String initialText = "aaa";
            model.replaceText(initialText);

            String deletionText = "bbb";
            String insertionText = "ccc";
            TextChange redo = tc(0, deletionText, insertionText);

            assert graph.isRedoValid(redo);
            BubbledRedoResult<TextChange> result = graph.bubbleRedo(redo);
            assert result.wasBubbled();
            assert result.getGrounded().equals(deletion(0, deletionText));
            assert result.getBubbled().equals(insertion(0, insertionText));
        }

        @Test
        public void bubblyLeftRedoIsBubbled() {
            String initialText = "aaa";
            String extraText = "bbb";
            String right = "ccc";
            model.replaceText(initialText + extraText);

            String text = "insertion";
            TextChange redo = tc(0, initialText + right, text);

            assert graph.isRedoValid(redo);
            BubbledRedoResult<TextChange> result = graph.bubbleRedo(redo);

            assert result.wasBubbled();
            assert result.getBubbled().equals(tc(0, initialText, text));
            assert result.getGrounded().equals(deletion(initialText.length(), right));
        }

        @Test
        public void bubblyRightRedoIsBubbled() {
            String extraText = "bbb";
            String initialText = "aaa";
            model.replaceText(extraText + initialText);

            String left = "ccc";
            String text = "insertion";
            TextChange redo = tc(0, left + initialText, text);

            assert graph.isRedoValid(redo);
            BubbledRedoResult<TextChange> result = graph.bubbleRedo(redo);

            assert result.wasBubbled();
            assert result.getGrounded().equals(deletion(redo.getStart(), left));
            assert result.getBubbled().equals(tc(extraText.length(), initialText, text));
        }

        @Test
        public void bubblyMiddleRedoIsBubbled() {
            String left = "aaa";
            String middle = "bbb";
            String right = "ccc";
            model.replaceText(middle);

            String text = "insertion";
            TextChange redo = tc(0, left + middle + right, text);

            assert graph.isRedoValid(redo);
            BubbledRedoResult<TextChange> result = graph.bubbleRedo(redo);

            assert result.wasBubbled();
            assert result.getBubbled().equals(tc(left.length(), middle, text));
            assert result.getGrounded().equals(deletion(0, left + right));
        }

    }

}
