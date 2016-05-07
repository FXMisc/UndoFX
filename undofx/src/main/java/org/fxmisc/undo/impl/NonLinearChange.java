package org.fxmisc.undo.impl;

public class NonLinearChange<S extends ChangeQueue<C>, C> extends RevisionedChange<C> {

    private final S source;

    public NonLinearChange(S source, C change, long rev) {
        super(change, rev);
        this.source = source;
    }

    public final S getSource() { return source; }

    public NonLinearChange<S, C> updateChange(C change) {
        return change.equals(getChange())
                ? this
                : new NonLinearChange<>(source, change, getRevision());
    }

    // TODO: Implement equals
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    // TODO: implement hashcode
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
