package org.fxmisc.undo;

import org.fxmisc.undo.impl.nonlinear.BubbledResult;
import org.fxmisc.undo.impl.nonlinear.NonlinearChangeQueue;
import org.reactfx.SuspendableNo;
import org.reactfx.util.TriFunction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface DirectedAcyclicGraph<C> {

    SuspendableNo performingActionProperty();

    boolean isPerformingAction();

    BiFunction<C, C, C> getUndoUpdater();

    Predicate<C> getIsValidUndo();

    TriFunction<C, C, BubbledResult<C>, C> getUndoUpdaterPostBubble();

    BiFunction<C, C, C> getRedoUpdater();

    Predicate<C> getIsValidRedo();

    TriFunction<C ,C, BubbledResult<C>, C> getRedoUpdaterPostBubble();

    boolean close();

    void registerQueue(NonlinearChangeQueue<C> queue);

    void unregisterQueue(NonlinearChangeQueue<C> queue);

    void setLatestChangeSource(NonlinearChangeQueue<C> source);

    NonlinearChangeQueue<C> getLatestChangeSource();

    BubbledResult<C> bubbleRedo(C change);

    BubbledResult<C> bubbleUndo(C change);

    void updateChangesWithPush(C pushedChange);

    void updateChangesWithRedo(NonlinearChangeQueue<C> source, C redo);

    void updateChangesPostUndoBubble(C original, BubbledResult<C> bubbledResult);

    void updateRedosPostRedoBubble(C original, BubbledResult<C> bubbledResult);

    void recalculateAllValidChanges();

    void remapEdges(C outdated, C updated);

    void testForDependency(C from, C to);

    void forget(List<C> changes);

    void removeRelatedEdgesOf(C change);

    boolean isMutuallyIndependent(C change);

}