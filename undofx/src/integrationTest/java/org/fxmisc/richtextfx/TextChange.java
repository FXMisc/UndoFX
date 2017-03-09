package org.fxmisc.richtextfx;

import java.util.Optional;

public class TextChange {

    public static enum ChangeType {
        /** Original change did (undo) or will (redo) insert something but not delete anything */
        INSERTION,
        /** Original change did (undo) or will (redo) delete something but not insert anything */
        DELETION,
        /** Original change did (undo) or will (redo) replace something */
        REPLACEMENT
    }

    private final int start;
    public final int getStart() { return start; }

    private final String inserted;
    public final String getInserted() { return inserted; }
    public final int insertedLength() { return inserted.length(); }
    public final int insertedEndPosition() { return start + insertedLength(); }

    private final String removed;
    public final String getRemoved() { return removed; }
    public final int removedLength() { return removed.length(); }
    public final int removedEndPosition() { return start + removedLength(); }

    private final ChangeType type;
    public final ChangeType getType() { return type; }

    public final int getDifference() { return insertedLength() - removedLength(); }

    public TextChange(int start, String removed, String inserted) {
        this.start = start;
        this.removed = removed;
        this.inserted = inserted;

        if (removed.isEmpty()) {
            if (!inserted.isEmpty()) {
                type = ChangeType.INSERTION;
            } else {
                throw new IllegalArgumentException("Cannot construct a change that doesn't delete or insert anything");
            }
        } else if (inserted.isEmpty()) {
            type = ChangeType.DELETION;
        } else {
            type = ChangeType.REPLACEMENT;
        }
    }

    public TextChange invert() {
        return new TextChange(start, inserted, removed);
    }

    public TextChange bumpPosition(int difference) {
        return difference == 0
                ? this
                : new TextChange(start + difference, removed, inserted);
    }

    public Optional<TextChange> mergeWith(TextChange otherChange) {
        if (this.insertedEndPosition() == otherChange.start) {
            // just "add" the two together
            String removedText = this.removed + otherChange.removed;
            String addedText = this.inserted + otherChange.inserted;
            return Optional.of(new TextChange(this.start, removedText, addedText));
        } else if (this.insertedEndPosition() == otherChange.removedEndPosition()) {
            // add one part of two together
            if (this.start <= otherChange.start) {
                // other change deletes part of the end of this change's insertion
                // -> add their insertions together
                String nonDeletedPortionOfThisChange = this.inserted.substring(0, otherChange.start - this.start);
                String addedText = nonDeletedPortionOfThisChange + otherChange.inserted;
                return Optional.of(new TextChange(this.start, this.removed, addedText));
            } else {
                // other change deletes the all of this change's insertion
                // -> act like this change's insertion never occurred
                // -> add their removals together
                String removalUpToThisChangesInsertion = otherChange.removed.substring(0, this.start - otherChange.start);
                String removedText = removalUpToThisChangesInsertion + this.removed;
                return Optional.of(new TextChange(otherChange.start, removedText, otherChange.inserted));
            }
        } else {
            // can't merge the two
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TextChange)) {
            return false;
        }

        TextChange that = (TextChange) obj;
        return this.start == that.start
                && this.removed.equals(that.removed)
                && this.inserted.equals(that.inserted);
    }

    @Override
    public int hashCode() {
        int result = 31 * start;
        result = 31 * result + inserted.hashCode();
        result = 31 * result + removed.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return toSingleLineString();
    }

    private String toSingleLineString() {
        return String.format("TextChange[type=%s, start=%s removedLength=%s removed=%s insertedLength=%s inserted=%s]",
                type, start, removedLength(), removed, insertedLength(), inserted);

    }

    private String toMultiLineString() {
        return String.format("TextChange[\n\tPosition=%s \n\tRemoved=%s \n\tInserted=%s\n]", start, removed, inserted);
    }
}
