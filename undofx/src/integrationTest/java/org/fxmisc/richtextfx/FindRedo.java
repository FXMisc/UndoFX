package org.fxmisc.richtextfx;

import javafx.scene.control.IndexRange;

import java.util.Optional;

public class FindRedo {

    public static Optional<IndexRange> findFirstValidRange(String removed, String modelText) {
        return new FindRedo(removed, modelText).findMatch();
    };

    private final String removed;
    private final String modelText;

    private FindRedo(String removed, String modelText) {
        this.removed = removed;
        this.modelText = modelText;
    }

    private boolean charactersMatch(int removedIndex, int modelIndex) {
        return removed.charAt(removedIndex) == modelText.charAt(modelIndex);
    }

    public Optional<IndexRange> findMatch() {
        for (int i = 0; i < removed.length(); i++) {
            for (int j = 0; j < modelText.length(); j++) {
                if (charactersMatch(i, j)) {
                    return findMatchEnd(i, j);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<IndexRange> findMatchEnd(int removedStartIndex, int modelStartIndex) {
        int removedEndIndex = removedStartIndex;
        int modelEndIndex = modelStartIndex;
        do {
            removedEndIndex++;
            modelEndIndex++;
        } while (removedEndIndex < removed.length()
                && modelEndIndex < modelText.length()
                && charactersMatch(removedEndIndex, modelEndIndex));

        return Optional.of(new IndexRange(removedStartIndex, removedEndIndex));
    }

}
