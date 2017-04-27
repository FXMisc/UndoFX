package org.fxmisc.undo.impl;

public interface ChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    boolean hasNext();

    boolean hasPrev();

    /** Returns the next item. Current position stays unchanged. */
    C peekNext();

    /** Returns the previous item. Current position stays unchanged. */
    C peekPrev();

    /** Returns the next item and increases the current position by 1. */
    C next();

    /** Returns the previous item and decreases the current position by 1. */
    C prev();

    @SuppressWarnings({"unchecked"})
    void push(C... changes);

    QueuePosition getCurrentPosition();

    void forgetHistory();
}
