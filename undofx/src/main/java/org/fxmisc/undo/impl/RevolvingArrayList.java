package org.fxmisc.undo.impl;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

class RevolvingArrayList<C> extends FixedSizeOverwritableListBase<C> {
    private final Object[] array;

    public final int getCapacity() { return array.length; }

    private int zeroIndex = 0;
    private int size = 0;

    RevolvingArrayList(int capacity) {
        array = new Object[capacity];
    }

    @SuppressWarnings("unchecked")
    C doGet(int index) {
        return (C) array[arrayIndex(index)];
    }

    @SuppressWarnings("unchecked")
    C doSet(int index, C element) {
        int arrayIndex = arrayIndex(index);
        C oldChange = (C) array[arrayIndex];
        array[arrayIndex] = element;
        return oldChange;
    }

    @Override
    public void add(int index, C element) {
        throwIfNegativeIndex(index);
        throwIfGreaterOrEqualToCapacity(index);

        if (size == getCapacity()) {
            if (index == size - 1) {
                overwriteZeroIndex(element);
            } else {
                addChangeAt(index, element);
                // don't increment size
            }
        } else {
            addChangeAt(index, element);
            size++;
        }
    }

    @Override
    public boolean add(C c) {
        if (size == getCapacity()) {
            overwriteZeroIndex(c);
        } else {
            addChangeAt(size, c);
            size++;
        }
        return true;
    }

    /** Adds a change at the given index and moves any changes to the right of index to the right by one */
    private void addChangeAt(int index, C element) {
        if (index < size) {
            moveChangesOneIndexRight(index);
        }
        array[arrayIndex(index)] = element;
    }

    /** When list is full and a change is being added, adds the change by overwriting the change at the lists' zero index */
    private void overwriteZeroIndex(C element) {
        array[zeroIndex] = element;

        if (zeroIndex == getCapacity() - 1) {
            zeroIndex = 0;
        } else {
            zeroIndex++;
        }
    }

    @SuppressWarnings("unchecked")
    C doRemove(int index) {
        int arrayIndex = arrayIndex(index);
        C oldChange = (C) array[arrayIndex];

        if (index + 1 == size) {
            array[arrayIndex] = null;
        } else {
            moveChangesOneIndexLeft(index + 1);
            array[arrayIndex(size - 1)] = null;
        }
        size--;
        return oldChange;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        zeroIndex = 0;
        size = 0;
    }

    @Override
    public Object[] toArray() {
        Object[] ar = new Object[getCapacity()];
        for (int i = 0; i < size; i++) {
            ar[i] = get(i);
        }
        return ar;
    }

    @Override
    public boolean removeIf(Predicate<? super C> filter) {
        boolean anyRemoved = false;
        for(int i = 0; i < size; i++) {
            C oldChange = doGet(i);
            if (filter.test(oldChange)) {
                doRemove(i);
                anyRemoved = true;
            }
        }
        return anyRemoved;
    }

    @Override
    public void replaceAll(UnaryOperator<C> operator) {
        for(int i = 0; i < size; i++) {
            C oldChange = doGet(i);
            C newChange = operator.apply(oldChange);
            doSet(i, newChange);
        }
    }

    private void moveChangesOneIndexRight(int index) {
        int moveStartIndex = arrayIndex(index);
        int endIndex = arrayIndex(size - 1);

        if (endIndex == getCapacity() - 1) {
            int rightSideArrayLength = getCapacity() - 1 - moveStartIndex;

            // move left side before right
            array[0] = array[getCapacity() - 1];
            if (rightSideArrayLength > 0) {
                System.arraycopy(array, moveStartIndex, array, moveStartIndex + 1, rightSideArrayLength - 1);
            }
        } else if (endIndex < moveStartIndex) {
            int rightSideArrayLength = getCapacity() - 1 - moveStartIndex;
            int leftSideArrayLength = endIndex + 1;

            // move left side before right
            if (leftSideArrayLength > 0) {
                System.arraycopy(array, 0, array, 1, leftSideArrayLength);
            }
            array[0] = array[getCapacity() - 1];
            if (rightSideArrayLength > 0) {
                System.arraycopy(array, moveStartIndex, array, moveStartIndex + 1, rightSideArrayLength - 1);
            }
        } else {
            System.arraycopy(array, moveStartIndex, array, moveStartIndex + 1, size - index);
        }
    }

    private void moveChangesOneIndexLeft(int index) {
        int moveStartIndex = arrayIndex(index);
        int endIndex = arrayIndex(size - 1);

        if (moveStartIndex == 0) {
            array[getCapacity() - 1] = array[0];
            System.arraycopy(array, 1, array, 0, endIndex);
        } else if (endIndex < moveStartIndex) {
            int rightSideArrayLength = getCapacity() - 1 - moveStartIndex;
            int leftSideArrayLength = endIndex + 1;

            // move right side before left
            if (rightSideArrayLength > 0) {
                System.arraycopy(array, moveStartIndex, array, moveStartIndex - 1, rightSideArrayLength);
            }
            array[getCapacity() - 1] = array[0];
            if (leftSideArrayLength > 0) {
                System.arraycopy(array, 1, array, 0, leftSideArrayLength);
            }
        } else {
            System.arraycopy(array, moveStartIndex, array, moveStartIndex - 1, size - index);
        }
    }

    private int arrayIndex(int index) {
        return (zeroIndex + index) % getCapacity();
    }

    private int relativize(int arrayIndex) {
        return (arrayIndex - zeroIndex + getCapacity()) % getCapacity();
    }

}
