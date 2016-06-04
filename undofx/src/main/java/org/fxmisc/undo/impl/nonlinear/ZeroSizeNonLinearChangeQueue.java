package org.fxmisc.undo.impl.nonlinear;

import org.fxmisc.undo.impl.linear.ZeroSizeLinearChangeQueue;

import java.util.Collections;
import java.util.List;

public class ZeroSizeNonLinearChangeQueue<C> extends ZeroSizeLinearChangeQueue<C> implements NonLinearChangeQueue<C> {

    @Override
    public List<C> getChanges() {
        return Collections.emptyList();
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public void recalculateValidChanges() {
        // nothing to do
    }

    @Override
    public void appliedChange() {
        // nothing to do
    }

    @Override
    public boolean committedLastChange() {
        return false;
    }

    @Override
    public void updateChanges(C pushedChange) {
        // nothing to do
    }

    @Override
    public void updateChangesPostBubble(C original, BubbledResult<C> bubbledResult) {
        // nothing to do
    }

    @Override
    public void updateRedosPostChangeBubble(C original, BubbledResult<C> bubbledResult) {
        // nothing to do
    }

}