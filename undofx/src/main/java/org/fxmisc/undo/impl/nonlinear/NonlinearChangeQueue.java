package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueue;

import java.util.List;

public interface NonlinearChangeQueue<C> extends ChangeQueue<C> {

    List<C> getChanges();

    void close();

    void recalculateValidChanges();

    void appliedChange();

    boolean committedLastChange();

    void updateChangesWithPush(C pushedChange);

    void updateChangesWithRedo(C redo);

    void updateChangesPostUndoBubble(C original, BubbledResult<C> bubbledResult);

    void updateRedosPostChangeBubble(C original, BubbledResult<C> bubbledResult);
}
