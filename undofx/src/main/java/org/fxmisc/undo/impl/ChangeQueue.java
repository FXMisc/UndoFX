package org.fxmisc.undo.impl;

public interface ChangeQueue<C> {

    interface QueuePosition {
        boolean isValid();
    }

    boolean hasNext();

    boolean hasPrev();

    C next();

    C prev();

    QueuePosition getCurrentPosition();

    void forgetHistory();
}
