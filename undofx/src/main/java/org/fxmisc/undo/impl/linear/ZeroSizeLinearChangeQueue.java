package org.fxmisc.undo.impl.linear;

import org.fxmisc.undo.impl.ChangeQueue;

import java.util.NoSuchElementException;

public class ZeroSizeLinearChangeQueue<C> implements ChangeQueue<C> {

    private class QueuePositionImpl implements QueuePosition {
        private final long rev;

        QueuePositionImpl(long seq) {
            this.rev = seq;
        }

        @Override
        public boolean isValid() {
            return rev == ZeroSizeLinearChangeQueue.this.revision;
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof ZeroSizeLinearChangeQueue.QueuePositionImpl) {
                @SuppressWarnings("unchecked")
                QueuePositionImpl otherPos = (QueuePositionImpl) other;
                return getQueue() == otherPos.getQueue() && rev == otherPos.rev;
            } else {
                return false;
            }
        }

        private ZeroSizeLinearChangeQueue<C> getQueue() {
            return ZeroSizeLinearChangeQueue.this;
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
    public C next() {
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
