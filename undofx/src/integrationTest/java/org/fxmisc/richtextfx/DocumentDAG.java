package org.fxmisc.richtextfx;

import javafx.scene.control.IndexRange;
import org.fxmisc.undo.impl.nonlinear.BubbledRedoResult;
import org.fxmisc.undo.impl.nonlinear.BubbledUndoResult;
import org.fxmisc.undo.impl.nonlinear.DirectedAcyclicGraphBase;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class DocumentDAG extends DirectedAcyclicGraphBase<TextChange, UndoBubbleType> {
    private final DocumentModel model;

    public DocumentDAG(DocumentModel model) {
        super();
        this.model = model;
    }

    /**
     * True if pushed change deletes a portion of undo's inserted. False if undo does not insert
     * anything or is the pushed change.
     */
    @Override
    public boolean firstIsModifiedBySecond(TextChange undo, TextChange pushedChange) {
        if (undo.equals(pushedChange) || undo.getType() == TextChange.ChangeType.DELETION) {
            return false;
        } else {
            int undoStart = undo.getStart();
            int undoInsertionEnd = undo.insertedEndPosition();
            int deletionStart = pushedChange.getStart();
            int deletionEnd = pushedChange.removedEndPosition();

            // true if deletes beginning
            return (deletionStart < undoStart && undoStart < deletionEnd)
                    // true if deletes end
                    || (deletionStart < undoInsertionEnd && undoInsertionEnd < deletionEnd)
                    // true if deletes middle
                    || (undoStart <= deletionStart && deletionEnd <= undoInsertionEnd && deletionStart != deletionEnd);
        }
    }

    @Override
    public TextChange updateUndo(TextChange outdatedUndo, TextChange pushedChange) {
        if (pushedChange.removedEndPosition() <= outdatedUndo.getStart()) {
            return outdatedUndo.bumpPosition(pushedChange.getDifference());
        } else {
            // either pushedChange occurs to the right of outdatedUndo
            // or it actually modifies outdatedUndo, in which case it might be bubbly
            return outdatedUndo;
        }
    }

    /**
     * True if an undo that only deleted something (undoing that change will removal nothing in
     * model text and re-insert what it deleted) or if undo's inserted portion shares at least
     * one character in the undo's change range in the model's text (undoing the change
     * will remove only that portion that matches the text).
     */
    @Override
    public boolean isUndoValid(TextChange undo) {
        switch (undo.getType()) {
            case DELETION:
                return true;
            default:    // insertion or replacement
                String inserted = undo.getInserted();
                String modelText = model.getText(undo.getStart(), undo.insertedEndPosition());

                return sharesAtLeastOneCharacterAtSameIndex(modelText, inserted);
        }
    }

    private boolean sharesAtLeastOneCharacterAtSameIndex(String modelText, String changeText) {
        for (int i = 0; i < changeText.length(); i++) {
            for (int j = 0; j < modelText.length(); j++) {
                if (changeText.charAt(i) == modelText.charAt(j)) {
                    return true;
                }
            }
        }

        return false; // if no character is found
    }

    /**
     * Should only be called with {@link TextChange.ChangeType#INSERTION} or {@link TextChange.ChangeType#REPLACEMENT}
     * undos. Splits the bubbly undo into a grounded and bubbled part.
     */
    @Override
    public BubbledUndoResult<TextChange, UndoBubbleType> doBubbleUndo(TextChange bubblyUndo, List<TextChange> dependencies) {
        if (dependencies.isEmpty()) {
            throw new IllegalStateException(format("The 'bubbly' undo had no dependencies but should have. " +
                    "undo=%s", bubblyUndo));
        }

        List<IndexRange> validUndoRanges = FindUndo.findAllValidUndoRanges(bubblyUndo, dependencies);

        if (validUndoRanges.isEmpty()) {
            throw new IllegalStateException(format("The 'bubbly' undo is not valid: dependencies modify entire change. " +
                    "undo=%s, dependencies=%s", bubblyUndo, dependencies));
        }

        String inserted = bubblyUndo.getInserted();
        String modelText = model.getText(bubblyUndo.getStart(), bubblyUndo.insertedEndPosition());

        IndexRange validUndoRange = null;
        int textStartIndex = 0, textEndIndex = 0;
        int modelTextStartIndex = -1;
        Iterator<IndexRange> iterator = validUndoRanges.iterator();
        while (modelTextStartIndex == -1 && iterator.hasNext()) {
            validUndoRange = iterator.next();

            // validUndoRange start >= bubblyUndo start
            textStartIndex = validUndoRange.getStart() - bubblyUndo.getStart();
            textEndIndex = textStartIndex + validUndoRange.getLength();

            String possibleValidInsertion = inserted.substring(textStartIndex, textEndIndex);
            modelTextStartIndex = modelText.indexOf(possibleValidInsertion);
        }

        if (modelTextStartIndex == -1) {
            throw new IllegalStateException(format("The 'bubbly' undo is not valid: no unmodified portion of the change " +
                    "matches the model's text. modelText=%s undo=%s dependencies=%s", modelText, bubblyUndo, dependencies));
        }

        TextChange bubbled, grounded;
        UndoBubbleType info;
        if (bubblyUndo.getStart() <= validUndoRange.getStart() && bubblyUndo.insertedEndPosition() == validUndoRange.getEnd()) {
            // grounded left & bubbly right
            String groundedText = inserted.substring(0, textStartIndex);
            String bubbledText = inserted.substring(textStartIndex);

            grounded = new TextChange(bubblyUndo.getStart(), "", groundedText);
            bubbled = new TextChange(modelTextStartIndex, bubblyUndo.getRemoved(), bubbledText);
            info = UndoBubbleType.GROUNDED_LEFT_BUBBLY_RIGHT;

        } else if (validUndoRange.getStart() == bubblyUndo.getStart() && validUndoRange.getEnd() <= bubblyUndo.insertedEndPosition()) {
            // bubbly left & grounded right
            String bubbledText = inserted.substring(0, textEndIndex);
            String groundedText = inserted.substring(textEndIndex);

            bubbled = new TextChange(modelTextStartIndex, bubblyUndo.getRemoved(), bubbledText);
            grounded = new TextChange(bubblyUndo.getStart(), "", groundedText);
            info = UndoBubbleType.BUBBLY_LEFT_GROUNDED_RIGHT;
        } else {
            // grounded left + right & bubbly middle
            String left = inserted.substring(0, textStartIndex);
            String mid = inserted.substring(textStartIndex, textEndIndex);
            String right = inserted.substring(textEndIndex);

            grounded = new TextChange(bubblyUndo.getStart(), "", left + right);
            bubbled = new TextChange(modelTextStartIndex, bubblyUndo.getRemoved(), mid);
            info = UndoBubbleType.BUBBLY_MIDDLE_GROUNDED_LEFT_PLUS_RIGHT;
        }
        return new BubbledUndoResult<>(grounded, bubbled, info);
    }

    @Override
    public TextChange updateBubbledUndoDependency(TextChange dependency, TextChange bubblyUndo,
                                                  BubbledUndoResult<TextChange, UndoBubbleType> result) {
        switch (result.getInfo()) {
            case GROUNDED_LEFT_BUBBLY_RIGHT:
                // nothing to modify, no position bump needed
                return dependency;
            case BUBBLY_LEFT_GROUNDED_RIGHT:
                // update start position: subtract the length of bubbled's insertion text
                return dependency.bumpPosition(-result.getBubbled().insertedLength());
            default: case BUBBLY_MIDDLE_GROUNDED_LEFT_PLUS_RIGHT:
                if (result.getBubbled().insertedEndPosition() <= dependency.getStart()) {
                    return dependency.bumpPosition(-result.getBubbled().insertedLength());
                } else {
                    return dependency;
                }
        }
    }

    @Override
    public TextChange updateDependency(TextChange dependency, TextChange oldChange, TextChange newChange) {
        return dependency.bumpPosition(newChange.getStart() - oldChange.getStart());
    }

    @Override
    public TextChange updateRedo(TextChange outdatedRedo, TextChange pushedChange) {
        if (pushedChange.removedEndPosition() <= outdatedRedo.getStart()) {
            return outdatedRedo.bumpPosition(pushedChange.getDifference());
        } else {
            // either pushedChange occurs to the right of outdatedUndo
            // or it actually modifies outdatedUndo, in which case it might be bubbly
            return outdatedRedo;
        }
    }

    @Override
    public boolean isRedoValid(TextChange redo) {
        switch (redo.getType()) {
            case DELETION:
                String removed = redo.getRemoved();
                String modelText = model.getText(redo.getStart(), redo.removedEndPosition());

                return sharesAtLeastOneCharacterAtSameIndex(modelText, removed);
            default: // insertion or replacement - a redo can always re-insert what it previously inserted
                return true;
        }
    }

    @Override
    public BubbledRedoResult<TextChange> bubbleRedo(TextChange redo) {
        if (redo.getType() == TextChange.ChangeType.INSERTION) {
            return BubbledRedoResult.noBubble();
        }

        String removed = redo.getRemoved();
        String modelText = model.getText(redo.getStart(), redo.removedEndPosition());

        if (modelText.equals(removed)) {
            return BubbledRedoResult.noBubble();
        } else {
            TextChange grounded, bubbled;
            Optional<IndexRange> validRange = FindRedo.findFirstValidRange(removed, modelText);
            if (!validRange.isPresent()) {
                // a redo can always be bubbled because it can always insert something, even if it removes nothing
                grounded = new TextChange(redo.getStart(), removed, "");
                bubbled = new TextChange(redo.getStart(), "", redo.getInserted());
            } else {
                IndexRange range = validRange.get();

                if (range.getStart() == 0) {
                    // bubbly left, grounded right
                    String bubbledText = removed.substring(0, range.getEnd());
                    String groundedText = removed.substring(range.getEnd());

                    bubbled = new TextChange(redo.getStart(), bubbledText, redo.getInserted());
                    grounded = new TextChange(redo.getStart() + range.getLength(), groundedText, "");
                } else if (range.getEnd() == removed.length()) {
                    // grounded left, bubbly right
                    String groundedText = removed.substring(0, range.getStart());
                    String bubbledText = removed.substring(range.getStart());

                    grounded = new TextChange(redo.getStart(), groundedText, "");
                    bubbled = new TextChange(redo.getStart() + range.getStart(), bubbledText, redo.getInserted());
                } else {
                    // bubbly middle, grounded left + right
                    String left = removed.substring(0, range.getStart());
                    String middle = removed.substring(range.getStart(), range.getEnd());
                    String right = removed.substring(range.getEnd());

                    grounded = new TextChange(redo.getStart(), left + right, "");
                    bubbled = new TextChange(redo.getStart() + left.length(), middle, redo.getInserted());
                }
            }
            return BubbledRedoResult.withBubble(grounded, bubbled);
        }
    }

}
