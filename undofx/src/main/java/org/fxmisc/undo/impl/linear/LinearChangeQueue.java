package org.fxmisc.undo.impl.linear;

import org.fxmisc.undo.impl.ChangeQueue;

public interface LinearChangeQueue<C> extends ChangeQueue<C> {

    @SuppressWarnings({"unchecked"})
    void push(C... changes);

}
