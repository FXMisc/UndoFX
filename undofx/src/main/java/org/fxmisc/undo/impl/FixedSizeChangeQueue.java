package org.fxmisc.undo.impl;

import java.util.NoSuchElementException;

public class FixedSizeChangeQueue<C> implements ChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final int arrayPos;
        private final long rev;

        QueuePositionImpl(int arrayPos, long rev) {
            this.arrayPos = arrayPos;
            this.rev = rev;
        }

        @Override
        public boolean isValid() {
            int pos = relativize(arrayPos);
            if(pos <= size) {
                return rev == fetchRevisionForPosition(pos)
                        // if the queue is full, then position 0 can also mean the last position ( = capacity)
                        || pos == 0 && size == capacity && rev == fetchRevisionForPosition(capacity);
            } else {
                return false;
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof FixedSizeChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && rev == otherPos.rev;
            } else {
                return false;
            }
        }

        private FixedSizeChangeQueue<C> getQueue() {
            return FixedSizeChangeQueue.this;
        }
    }

    private final RevisionedChange<C>[] changes;
    private final int capacity;
    private int start = 0;
    private int size = 0;

    // current position is always from the interval [0, size],
    // i.e. not offset by start
    private int currentPosition = 0;

    private long revision = 0;
    private long zeroPositionRevision = revision;

    @SuppressWarnings("unchecked")
    public FixedSizeChangeQueue(int capacity) {
        if(capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }

        this.capacity = capacity;
        this.changes = new RevisionedChange[capacity];
    }

    @Override
    public boolean hasNext() {
        return currentPosition < size;
    }

    @Override
    public boolean hasPrev() {
        return currentPosition > 0;
    }

    @Override
    public C peekNext() {
        if(currentPosition < size) {
            return fetch(currentPosition).getChange();
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public C next() {
        C c = peekNext();
        currentPosition += 1;
        return c;
    }

    @Override
    public C peekPrev() {
        if(currentPosition > 0) {
            return fetch(currentPosition - 1).getChange();
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public C prev() {
        C c = peekPrev();
        currentPosition -= 1;
        return c;
    }

    @Override
    public void forgetHistory() {
        zeroPositionRevision = fetchRevisionForPosition(currentPosition);
        start = arrayIndex(currentPosition);
        size -= currentPosition;
        currentPosition = 0;
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        RevisionedChange<C> lastOverwrittenChange = null;
        for(C c: changes) {
            RevisionedChange<C> revC = new RevisionedChange<>(c, ++revision);
            lastOverwrittenChange = put(currentPosition++, revC);
        }

        if(currentPosition > capacity) {
            start = arrayIndex(currentPosition);
            currentPosition = capacity;
            size = capacity;
            zeroPositionRevision = lastOverwrittenChange.getRevision();
        } else {
            size = currentPosition;
        }
    }

    @Override
    public QueuePosition getCurrentPosition() {
        long rev = fetchRevisionForPosition(currentPosition);
        return new QueuePositionImpl(arrayIndex(currentPosition), rev);
    }

    private long fetchRevisionForPosition(int position) {
        if(position == 0) {
            return zeroPositionRevision;
        } else {
            return fetch(position - 1).getRevision();
        }
    }

    private RevisionedChange<C> fetch(int position) {
        return changes[arrayIndex(position)];
    }

    private RevisionedChange<C> put(int position, RevisionedChange<C> c) {
        RevisionedChange<C> old = changes[arrayIndex(position)];
        changes[arrayIndex(position)] = c;
        return old;
    }

    // returns a number from [0..capacity-1]
    private int arrayIndex(int queuePosition) {
        return (start + queuePosition) % capacity;
    }

    // inverse of arrayPos
    private int relativize(int arrayPos) {
        return (arrayPos - start + capacity) % capacity;
    }
}
