package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.ChangeQueue;

import java.util.List;

public interface NonLinearChangeQueue<C> extends ChangeQueue<C> {

    public List<C> getChanges();
    
    public void close();
    
    public void recalculateValidChanges();

    public void appliedChange();
    
    public boolean committedLastChange();

    public void updateChanges(C pushedChange);

    public void updateChangesPostBubble(C original, BubbledResult<C> bubbledResult);

    public void updateRedosPostChangeBubble(C original, BubbledResult<C> bubbledResult);
}
