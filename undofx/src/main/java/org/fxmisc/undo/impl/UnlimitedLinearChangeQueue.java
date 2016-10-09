package org.fxmisc.undo.impl;

import java.util.ArrayList;

public class UnlimitedLinearChangeQueue<C> extends ChangeQueueBase<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final int allTimePos;
        private final long rev;

        QueuePositionImpl(int allTimePos, long rev) {
            this.allTimePos = allTimePos;
            this.rev = rev;
        }

        @Override
        public boolean isValid() {
            int pos = allTimePos - forgottenCount;
            if(0 <= pos && pos <= changes.size()) {
                return rev == revisionForPosition(pos);
            } else {
                return false;
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof UnlimitedLinearChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && rev == otherPos.rev;
            } else {
                return false;
            }
        }

        private UnlimitedLinearChangeQueue<C> getQueue() {
            return UnlimitedLinearChangeQueue.this;
        }
    }

    private final ArrayList<RevisionedChange<C>> changes = new ArrayList<>();
    private int currentPosition = 0;

    private long revision = 0;
    private long zeroPositionRevision = revision;
    private int forgottenCount = 0;

    public UnlimitedLinearChangeQueue() {
        this.mark = getCurrentPosition();
    }

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
        int index = currentPosition++;
        invalidateBindings();
        return changes.get(index).getChange();
    }

    @Override
    public final C prev() {
        int index = --currentPosition;
        invalidateBindings();
        return changes.get(index).getChange();
    }

    @Override
    public void forgetHistory() {
        if(currentPosition > 0) {
            zeroPositionRevision = revisionForPosition(currentPosition);
            int newSize = changes.size() - currentPosition;
            for(int i = 0; i < newSize; ++i) {
                changes.set(i, changes.get(currentPosition + i));
            }
            changes.subList(newSize, changes.size()).clear();
            forgottenCount += currentPosition;
            currentPosition = 0;
            undoAvailable.invalidate();
        }
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
        invalidateBindings();
    }

    @Override
    public QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(forgottenCount + currentPosition, revisionForPosition(currentPosition));
    }

    private long revisionForPosition(int position) {
        return position == 0
                ? zeroPositionRevision
                : changes.get(position - 1).getRevision();
    }
}
