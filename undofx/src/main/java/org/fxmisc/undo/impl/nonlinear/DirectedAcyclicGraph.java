package org.fxmisc.undo.impl.nonlinear;

import org.reactfx.SuspendableNo;
import org.reactfx.util.TriFunction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface DirectedAcyclicGraph<C> {

    public SuspendableNo performingActionProperty();

    public boolean isPerformingAction();

    public BiFunction<C, C, C> getUndoUpdater();

    public Predicate<C> getIsValidUndo();

    public TriFunction<C, C, BubbledResult<C>, C> getUndoUpdaterPostBubble();

    public BiFunction<C, C, C> getRedoUpdater();

    public Predicate<C> getIsValidRedo();

    public TriFunction<C ,C, BubbledResult<C>, C> getRedoUpdaterPostBubble();

    public boolean close();

    public void registerQueue(NonLinearChangeQueue<C> queue);

    public void unregisterQueue(NonLinearChangeQueue<C> queue);

    public void setLatestChangeSource(NonLinearChangeQueue<C> source);

    public NonLinearChangeQueue<C> getLatestChangeSource();

    public BubbledResult<C> bubbleRedo(C change);

    public BubbledResult<C> bubbleUndo(C change);

    public void updateChangesPostUndoBubble(C original, BubbledResult<C> bubbledResult);

    public void updateRedosPostRedoBubble(C original, BubbledResult<C> bubbledResult);

    public void updateQueueChanges(C pushedChange);

    public void recalculateAllValidChanges();

    public void remapEdges(C outdated, C updated);

    public void testForDependency(C from, C to);

    public void forget(List<C> changes);

    public void removeRelatedEdgesOf(C change);

    public boolean isMutuallyIndependent(C change);
    
}
