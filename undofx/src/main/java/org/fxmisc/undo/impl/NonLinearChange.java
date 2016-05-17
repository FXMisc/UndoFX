package org.fxmisc.undo.impl;

import java.util.Objects;

public class NonLinearChange<S extends NonLinearChangeQueue<C>, C> extends RevisionedChange<C> {

    private final S source;

    public NonLinearChange(S source, C change, long rev) {
        super(change, rev);
        this.source = source;
    }

    public final S getSource() { return source; }

    public NonLinearChange<S, C> updateChange(C change) {
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
