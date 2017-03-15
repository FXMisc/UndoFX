package org.fxmisc.undo.impl;

import java.util.List;

/**
 * A fixed size list that neither grows nor shrinks; once full, any new change added to it will overwrite the
 * first change stored.
 *
 * @param <E> type of element stored in list
 */
public interface FixedSizeOverwritableList<E> extends List<E> {

    public static <E> FixedSizeOverwritableList<E> withCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than or equal to 1 but was " + capacity);
        } else if(capacity == 1) {
            return new SingletonList<E>();
        } else {
            return new RevolvingArrayList<E>(capacity);
        }
    }

    /** Returns the capacity of the list */
    public int getCapacity();

}
