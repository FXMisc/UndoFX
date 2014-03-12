package org.fxmisc.undo.impl;

public interface ChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    boolean hasNext();

    boolean hasPrev();

    C next();

    C prev();

    @SuppressWarnings({"unchecked"})
    void push(C... changes);

    QueuePosition getCurrentPosition();

    void forgetHistory();
}
