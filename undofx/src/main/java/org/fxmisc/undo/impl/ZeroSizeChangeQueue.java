package org.fxmisc.undo.impl;

import java.util.NoSuchElementException;

public class ZeroSizeChangeQueue<C> implements ChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final long rev;

        QueuePositionImpl(long seq) {
            this.rev = seq;
        }

        @Override
        public boolean isValid() {
            return rev == ZeroSizeChangeQueue.this.revision;
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof ZeroSizeChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && rev == otherPos.rev;
            } else {
                return false;
            }
        }

        private ZeroSizeChangeQueue<C> getQueue() {
            return ZeroSizeChangeQueue.this;
        }
    }

    private long revision = 0;

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrev() {
        return false;
    }

    @Override
    public C peekNext() {
        throw new NoSuchElementException();
    }

    @Override
    public C next() {
        throw new NoSuchElementException();
    }

    @Override
    public C peekPrev() {
        throw new NoSuchElementException();
    }

    @Override
    public C prev() {
        throw new NoSuchElementException();
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        ++revision;
    }

    @Override
    public QueuePosition getCurrentPosition() {
        return new QueuePositionImpl(revision);
    }

    @Override
    public void forgetHistory() {
        // there is nothing to forget
    }
}
