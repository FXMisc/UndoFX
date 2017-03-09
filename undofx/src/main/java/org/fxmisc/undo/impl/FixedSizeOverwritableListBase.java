package org.fxmisc.undo.impl;

import java.util.AbstractList;

abstract class FixedSizeOverwritableListBase<E> extends AbstractList<E> implements FixedSizeOverwritableList<E> {

    FixedSizeOverwritableListBase() {}

    @Override
    public E get(int index) {
        throwIfNegativeIndex(index);
        throwIfGreaterOrEqualToSize(index);

        return doGet(index);
    }

    abstract E doGet(int index);

    @Override
    public E set(int index, E element) {
        throwIfNegativeIndex(index);
        throwIfGreaterOrEqualToSize(index);

        return doSet(index, element);
    }

    abstract E doSet(int index, E element);

    @Override
    public E remove(int index) {
        throwIfNegativeIndex(index);
        throwIfGreaterOrEqualToSize(index);

        return doRemove(index);
    }

    abstract E doRemove(int index);

    void throwIfNegativeIndex(int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index was negative: " + index);
        }
    }

    void throwIfGreaterOrEqualToCapacity(int index) {
        if (index >= getCapacity()) {
            throw new ArrayIndexOutOfBoundsException("Array has a capacity of " + getCapacity() + " but index was " + index);
        }
    }

    void throwIfGreaterOrEqualToSize(int index) {
        if (index >= size()) {
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is greater than or equal to the number of " +
                    "elements currently stored. Size = " + size());
        }
    }

}