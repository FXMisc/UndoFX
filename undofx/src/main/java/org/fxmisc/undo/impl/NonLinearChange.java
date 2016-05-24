package org.fxmisc.undo.impl;

import java.util.Objects;

public class NonLinearChange<C> extends RevisionedChange<C> {

    private final NonLinearChangeQueue<C> source;

    public NonLinearChange(NonLinearChangeQueue<C> source, C change, long rev) {
        super(change, rev);
        this.source = source;
    }

    public final NonLinearChangeQueue<C> getSource() { return source; }

    public NonLinearChange<C> updateChange(C change) {
        return new NonLinearChange<>(source, change, getRevision());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NonLinearChange)) {
            return false;
        }
        NonLinearChange that = (NonLinearChange) obj;
        return Objects.equals(this.source, that.source)
                && Objects.equals(this.getChange(), that.getChange())
                && this.getRevision() == that.getRevision();
    }

    @Override
    public int hashCode() {
        int result = 31 * source.hashCode();
        result = 31 * result + getChange().hashCode();
        result = 31 * result + Long.hashCode(getRevision());
        return result;
    }
}
