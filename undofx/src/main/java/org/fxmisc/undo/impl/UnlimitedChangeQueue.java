package org.fxmisc.undo.impl;

import java.util.ArrayList;

public class UnlimitedChangeQueue<C> implements ChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final int pos;
        private final long rev;

        QueuePositionImpl(int pos, long rev) {
            this.pos = pos;
            this.rev = rev;
        }

        @Override
        public boolean isValid() {
            if(pos <= changes.size()) {
                return rev == revisionForPosition(pos);
            } else {
                return false;
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof UnlimitedChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && rev == otherPos.rev;
            } else {
                return false;
            }
        }

        private UnlimitedChangeQueue<C> getQueue() {
            return UnlimitedChangeQueue.this;
        }
    }

    private final ArrayList<RevisionedChange<C>> changes = new ArrayList<>();
    private int currentPosition = 0;

    private long revision = 0;
    private final long zeroPositionRevision = revision;

    @Override
    public final boolean hasNext() {
        return currentPosition < changes.size();
    }

    @Override
    public final boolean hasPrev() {
        return currentPosition > 0;
    }

    @Override
    public final C next() {
        return changes.get(currentPosition++).getChange();
    }

    @Override
    public final C prev() {
        return changes.get(--currentPosition).getChange();
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        this.changes.subList(currentPosition, this.changes.size()).clear();
        for(C c: changes) {
            RevisionedChange<C> revC = new RevisionedChange<>(c, ++revision);
            this.changes.add(revC);
        }
        currentPosition += changes.length;
    }

    @Override
    public QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(currentPosition, revisionForPosition(currentPosition));
    }

    private long revisionForPosition(int position) {
        return position == 0
                ? zeroPositionRevision
                : changes.get(position - 1).getRevision();
    }
}
