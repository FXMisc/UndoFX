package org.fxmisc.undo.impl.nonlinear;

public class IndexedChange<C> {

    private final int index;
    public final int getIndex() { return index; }

    private final C change;
    public final C getChange() { return change; }

    public IndexedChange(int index, C change) {
        this.index = index;
        this.change = change;
    }

}
