package org.fxmisc.undo.impl;

class RevisionedChange<C> {
    private final C change;
    private final long revision;

    public RevisionedChange(C change, long revision) {
        this.change = change;
        this.revision = revision;
    }

    public C getChange() {
        return change;
    }

    public long getRevision() {
        return revision;
    }
}
