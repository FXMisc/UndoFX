package org.fxmisc.undo.impl;

import javafx.beans.binding.BooleanBinding;
import javafx.collections.transformation.FilteredList;
import org.reactfx.SuspendableNo;

import java.util.ArrayList;
import java.util.List;

public class NonLinearUnlimitedChangeQueue<C> implements NonLinearChangeQueue<C> {

    // TODO: This still needs to be properly implemented (copied from UnlimitedLinearChangeQueue
    private class NonLinearQueuePositionImpl implements ChangeQueue.QueuePosition {
        private final int allTimePos;
        private final long rev;

        NonLinearQueuePositionImpl(int allTimePos, long rev) {
            this.allTimePos = allTimePos;
            this.rev = rev;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
    }

    // TODO: Test this
    // A replacement for UndoManager#canMerge: a merge should only be able to occur when this queue made the last change.
    // Otherwise, it might be outdated or conflict with some other change from another queue.
    private final BooleanBinding committedLastChange = new BooleanBinding() {
        @Override
        protected boolean computeValue() {
            return graph.getLastChangeSource().equals(NonLinearUnlimitedChangeQueue.this);
        }
    };
    public final boolean committedLastChange() { return committedLastChange.get(); }

    public SuspendableNo performingActionProperty() { return graph.performingActionProperty(); }
    public boolean isPerformingAction() { return graph.isPerformingAction(); }

    // TODO: Should this still be used for equals? (NonLinearUndoManagerFactory can pass a new ID each time or something, but there may be better way
    private final int id;
    final int getId() { return id; }

    private final DirectAcyclicGraphImpl<NonLinearUnlimitedChangeQueue<C>, C> graph;
    private final FilteredList<NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C>> changes;

    // TODO: Still need to figure out how to adjust Current Position due to its relative position now
    private int currentPosition = 0;
    private long revision = 0;
    private long zeroPositionRevision = revision;
    private int forgottenCount = 0;

    public NonLinearUnlimitedChangeQueue(int id, DirectAcyclicGraphImpl<NonLinearUnlimitedChangeQueue<C>, C> graph) {
        this.id = id;
        this.graph = graph;
        changes = graph.getChangesFor(this);
    }

    @Override
    public boolean hasNext() {
        return currentPosition < changes.size();
    }

    @Override
    public boolean hasPrev() {
        return currentPosition > 0;
    }

    @Override
    public C next() {
        return changes.get(currentPosition++).getChange();
    }

    @Override
    public C prev() {
        NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C> previous = changes.get(--currentPosition);
        return graph.getValidChangeFor(previous, () -> revision++).getChange();
    }

    @Override
    @SafeVarargs
    public final void push(C... changes) {
        List<NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C>> redoList = this.changes.subList(currentPosition, this.changes.size());
        List<NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C>> newChanges = new ArrayList<>(changes.length);
        for (C c : changes) {
            NonLinearChange<NonLinearUnlimitedChangeQueue<C>, C> nonLinearC = new NonLinearChange<>(this, c, ++revision);
            newChanges.add(nonLinearC);
        }
        graph.push(redoList, newChanges);
        currentPosition += changes.length;
    }

    // TODO: implement this correctly
    @Override
    public ChangeQueue.QueuePosition getCurrentPosition() {
        return null;
    }

    @Override
    public void forgetHistory() {
        if(currentPosition > 0) {
            zeroPositionRevision = revisionForPosition(currentPosition);
            int newSize = changes.size() - currentPosition;
            graph.forgetChanges(changes.subList(0, newSize));
            forgottenCount += currentPosition;
            currentPosition = 0;
        }
    }

    // TODO: Is this still needed if Current Position is now relative? (Will it even be relevant?)
    private long revisionForPosition(int position) {
        return position == 0
                ? zeroPositionRevision
                : changes.get(position - 1).getRevision();
    }

    // TODO: Figure out best way to implement equals here... Perhaps using DAG in some way?
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NonLinearUnlimitedChangeQueue) {
            NonLinearUnlimitedChangeQueue that = (NonLinearUnlimitedChangeQueue) obj;
            return this.id == that.getId();
        } else {
            return false;
        }
    }

}
