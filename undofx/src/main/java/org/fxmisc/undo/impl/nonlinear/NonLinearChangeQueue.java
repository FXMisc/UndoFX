package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueue;

import java.util.List;

public interface NonLinearChangeQueue<C> extends ChangeQueue<C> {

    public List<C> getChanges();
    
    public void close();
    
    public void recalculateValidChanges();

    public void appliedRedo(C redo);
    
    public boolean committedLastChange();

    public void updateChangesWithPush(C pushedChange);

    public void updateChangesWithRedo(C redo);

    public void updateChangesPostUndoBubble(C original, BubbledResult<C> bubbledResult);

    public void updateRedosPostChangeBubble(C original, BubbledResult<C> bubbledResult);
}
