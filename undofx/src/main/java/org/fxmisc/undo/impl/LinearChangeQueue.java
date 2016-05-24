package org.fxmisc.undo.impl;

public interface LinearChangeQueue<C> extends ChangeQueue<C> {

    @SuppressWarnings({"unchecked"})
    void push(C... changes);
}
