package org.fxmisc.undo.impl;

class SingletonList<C> extends FixedSizeOverwritableListBase<C> {

    private C change = null;

    SingletonList() {}

    @Override
    public int size() {
        return change == null ? 0 : 1;
    }

    @Override
    C doGet(int index) {
        return change;
    }

    @Override
    C doRemove(int index) {
        C oldChange = change;
        change = null;
        return oldChange;
    }

    @Override
    C doSet(int index, C element) {
        C oldChange = change;
        change = element;
        return oldChange;
    }

    @Override
    public boolean add(C c) {
        change = c;
        return true;
    }

    @Override
    public void add(int index, C element) {
        throwIfNegativeIndex(index);
        throwIfGreaterOrEqualToCapacity(index);

        change = element;
    }

    @Override
    public int getCapacity() {
        return 1;
    }
}