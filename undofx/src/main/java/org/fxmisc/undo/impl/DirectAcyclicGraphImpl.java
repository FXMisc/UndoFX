package org.fxmisc.undo.impl;

import org.fxmisc.undo.DirectedAcyclicUndoGraph;
import org.reactfx.SuspendableNo;
import org.reactfx.util.TriFunction;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DirectAcyclicGraphImpl<Source extends NonLinearChangeQueue<C>, C> implements DirectedAcyclicUndoGraph<Source, C> {

    private final List<NonLinearChange<Source, C>> EMPTY_LIST = new ArrayList<>(0);

    private final List<NonLinearChange<Source, C>> redosAll = new ArrayList<>(1);
    private List<NonLinearChange<Source, C>> redosValid = new ArrayList<>(1);

    private final List<NonLinearChange<Source, C>> undosAll = new ArrayList<>(1);
    private List<NonLinearChange<Source, C>> undosValid = new ArrayList<>(1);

    private final HashMap<NonLinearChange<Source, C>, List<NonLinearChange<Source, C>>> toFromEdges = new HashMap<>();

    private final SuspendableNo performingAction = new SuspendableNo();
    public final SuspendableNo performingActionProperty() {
        return performingAction;
    }

    private final BiPredicate<C, C> firstDependsOnSecond;

    private final BiFunction<C, C, C> undoUpdater;
    private final Predicate<C> isValidUndo;
    private final BiFunction<C, List<C>, BubbledResult<C>> undoBubbler;
    private final TriFunction<C, C, BubbledResult<C>, C> postBubbleUndoUpdater;

    private final BiFunction<C, C, C> redoUpdater;
    private final Predicate<C> isValidRedo;
    private final Function<C, BubbledResult<C>> redoBubbler;
    private final TriFunction<C, C, BubbledResult<C>, C> postBubbleRedoUpdater;

    private long revision = 0;

    public DirectAcyclicGraphImpl(
            BiPredicate<C, C> firstDependsOnSecond,
            BiFunction<C, C, C> undoUpdater,
            Predicate<C> isValidUndo,
            BiFunction<C, List<C>, BubbledResult<C>> undoBubbler,
            TriFunction<C, C, BubbledResult<C>, C> postBubbleUndoUpdater,
            BiFunction<C, C, C> redoUpdater,
            Predicate<C> isValidRedo,
            Function<C, BubbledResult<C>> redoBubbler,
            TriFunction<C, C, BubbledResult<C>, C> postBubbleRedoUpdater) {
        this.firstDependsOnSecond = firstDependsOnSecond;

        this.undoUpdater = undoUpdater;
        this.isValidUndo = isValidUndo;
        this.undoBubbler = undoBubbler;
        this.postBubbleUndoUpdater = postBubbleUndoUpdater;

        this.redoUpdater = redoUpdater;
        this.isValidRedo = isValidRedo;
        this.redoBubbler = redoBubbler;
        this.postBubbleRedoUpdater = postBubbleRedoUpdater;
    }

    public final boolean hasNextFor(Source source) {
        boolean hasNext = false;
        for (int i = redosValid.size() - 1; i >= 0; i--) {
            if (redosValid.get(i).getSource() == source) {
                hasNext = true;
                break;
            }
        }
        return hasNext;
    }

    public final boolean hasPrevFor(Source source) {
        boolean hasPrev = false;
        for (int i = undosValid.size() - 1; i >= 0; i--) {
            if (undosValid.get(i).getSource() == source) {
                hasPrev = true;
                break;
            }
        }
        return hasPrev;
    };

    public final C nextFor(Source source) {
        NonLinearChange<Source, C> validRedo = null;
        for (int i = redosValid.size() - 1; i >= 0; i--) {
            NonLinearChange<Source, C> possibleChange = redosValid.get(i);
            if (possibleChange.getSource() == source) {
                validRedo = possibleChange;
                break;
            }
        }
        if (validRedo == null) {
            throw new IllegalStateException("Method 'nextFor(Source)' should only be called when" +
                    "method 'hasNextFor(Source)' returns true, but was called when no valid redo was found");
        } else {
            return getValidFormOfRedo(validRedo);
        }
    }

    public final C prevFor(Source source) {
        NonLinearChange<Source, C> validUndo = null;
        for (int i = undosValid.size() - 1; i >= 0; i--) {
            NonLinearChange<Source, C> change = undosValid.get(i);
            if (change.getSource() == source) {
                validUndo = change;
                break;
            }
        }
        if (validUndo == null) {
            throw new IllegalStateException("Method 'prevFor(Source)' should only be called when" +
                    "method 'hasPrevFor(Source)' returns true, but was called when no valid undo was found.");
        } else {
            return getValidFormOfUndo(validUndo);
        }
    }

    private long expectedRedoRevision;

    private C getValidFormOfRedo(NonLinearChange<Source, C> validRedo) {
        C originalChange = validRedo.getChange();
        BubbledResult<C> bubbledResult = redoBubbler.apply(originalChange);
        C bubbledChange = bubbledResult.getBubbled();

        if (originalChange.equals(bubbledChange)) {
            redosAll.remove(validRedo);
            expectedRedoRevision = validRedo.getRevision();
            return originalChange;
        } else {
            int index = redosAll.indexOf(validRedo);
            NonLinearChange<Source, C> updated = validRedo.updateChange(bubbledResult.getBuried());

            updateRedosPostBubble(updated, originalChange, bubbledResult);

            redosAll.set(index, validRedo.updateChange(bubbledResult.getBuried()));
            expectedRedoRevision = revision++;
            return bubbledChange;
        }
    }

    private void updateRedosPostBubble(NonLinearChange<Source, C> preBubbledChange, C original, BubbledResult<C> bubbleResult) {
        for (int i = 0; i < redosAll.size() - 1; i++ ) {
            NonLinearChange<Source, C> outdatedChange = redosAll.get(i);
            if (!preBubbledChange.equals(outdatedChange)) {
                C outdated = outdatedChange.getChange();
                C updated = postBubbleRedoUpdater.apply(outdated, original, bubbleResult);
                if (!outdated.equals(updated)) {
                    NonLinearChange<Source, C> updatedChange = outdatedChange.updateChange(updated);
                    redosAll.set(i, updatedChange);
                }
            }
        }
    }

    private long expectedUndoRevision;

    private C getValidFormOfUndo(NonLinearChange<Source, C> undoableChange) {
        List<NonLinearChange<Source, C>> dependencies = getDependencies(undoableChange);
        boolean isMutuallyIndependent = dependencies.isEmpty();
        if (isMutuallyIndependent) {
            removeRelatedEdgesOf(undoableChange);
            undosAll.remove(undoableChange);
            expectedUndoRevision = undoableChange.getRevision();

            return undoableChange.getChange();
        } else {
            List<C> dependencyChanges = extractChangesFrom(dependencies);
            C original = undoableChange.getChange();
            BubbledResult<C> bubbleResult = undoBubbler.apply(original, dependencyChanges);

            updateUndosPostBubble(undoableChange, original, bubbleResult);

            NonLinearChange<Source, C> buriedChange = undoableChange.updateChange(bubbleResult.getBuried());
            int index = undosAll.indexOf(undoableChange);
            undosAll.set(index, buriedChange);
            remapAllEdges(undoableChange, buriedChange);

            expectedUndoRevision = revision++;

            return bubbleResult.getBubbled();
        }
    }

    private void updateUndosPostBubble(NonLinearChange<Source, C> change, C original, BubbledResult<C> bubbleResult) {
        for (int i = 0; i < undosAll.size(); i++) {
            NonLinearChange<Source, C> outdatedChange = undosAll.get(i);
            if (!change.equals(outdatedChange)) {
                C outdated = outdatedChange.getChange();
                C updated = postBubbleUndoUpdater.apply(outdated, original, bubbleResult);
                if (!outdated.equals(updated)) {
                    NonLinearChange<Source, C> updatedChange = outdatedChange.updateChange(updated);
                    undosAll.set(i, updatedChange);
                    remapAllEdges(outdatedChange, updatedChange);
                }
            }
        }
    }

    public void closeDown(Source source) {
        redosAll.stream()
                .filter(changesDoneBy(source))
                .forEach(redosAll::remove);

        undosAll.stream()
                .filter(changesDoneBy(source))
                .forEach(c -> {
            removeRelatedEdgesOf(c);
            undosAll.remove(c);
        });
    }

    public void forgetHistoryFor(Source source, Consumer<Void> ifForgotten) {
        List<NonLinearChange<Source, C>> history = undosAll.stream()
                .filter(changesDoneBy(source))
                .collect(Collectors.toCollection(ArrayList::new));

        if (!history.isEmpty()) {
            history.subList(0, history.size() - 1).forEach(c -> {
                removeRelatedEdgesOf(c);
                undosAll.remove(c);
            });
            ifForgotten.accept(null);
        }
    }


    @SafeVarargs
    public final void pushChanges(Source source, C... newChanges) {
        redosValid.stream()
                .filter(changesDoneBy(source))
                .forEach(redosValid::remove);

        push(source, () -> revision++, newChanges);
    }

    public final void pushRedo(Source source, C redoChange) {
        push(source, () -> expectedRedoRevision, redoChange);
    }

    public void addRedoFor(Source source, C change) {
        NonLinearChange<Source, C> redo = new NonLinearChange<>(source, change, expectedUndoRevision);
        redosAll.add(redo);
        redosValid.add(redo);
    }

    @SafeVarargs
    private final void push(Source source, Supplier<Long> revisionSupplier, C... newChanges) {
        Stream.of(newChanges).forEach(c -> {
            updateRedoableChanges(c);
            updateUndoableChanges(source, c, revisionSupplier);
        });
        recalculateValidChanges();
    }

    private void updateRedoableChanges(C change) {
        for (int i = 0; i < redosAll.size(); i++) {
            NonLinearChange<Source, C> outdatedChange = redosAll.get(i);
            C outdated = outdatedChange.getChange();
            C updated = redoUpdater.apply(change, outdated);
            if (!outdated.equals(updated)) {
                redosAll.set(i, outdatedChange.updateChange(updated));
            }
        }
    }

    private void updateUndoableChanges(Source source, C change, Supplier<Long> revisionSupplier) {
        NonLinearChange<Source, C> addedChange = new NonLinearChange<>(source, change, revisionSupplier.get());

        if (!undosAll.isEmpty()) {
            for (int i = 0; i < undosAll.size(); i++) {
                NonLinearChange<Source, C> outdatedChange = undosAll.get(i);
                C outdated = outdatedChange.getChange();
                C updated = undoUpdater.apply(change, outdated);

                NonLinearChange<Source, C> updatedChange;
                if (!outdated.equals(updated)) {
                    updatedChange = outdatedChange.updateChange(updated);
                    undosAll.set(i, updatedChange);
                    remapAllEdges(outdatedChange, updatedChange);

                    int index = undosValid.indexOf(outdatedChange);
                    if (index >= 0) {
                        undosValid.set(index, updatedChange);
                    }
                } else {
                    updatedChange = outdatedChange;
                }

                if (firstDependsOnSecond.test(change, updated)) {
                    addEdgeFromTo(addedChange, updatedChange);
                }
            }
        }

        undosAll.add(addedChange);
    }

    private void removeRelatedEdgesOf(NonLinearChange<Source, C> target) {
        for (NonLinearChange<Source, C> key : toFromEdges.keySet()) {
            if (key.equals(target)) {
                toFromEdges.remove(key);
            } else {
                toFromEdges.get(target).remove(target);
            }
        }
    }

    private void addEdgeFromTo(NonLinearChange<Source, C> from, NonLinearChange<Source, C> to) {
        if (toFromEdges.containsKey(to)) {
            toFromEdges.get(to).add(from);
        } else {
            ArrayList<NonLinearChange<Source, C>> list = new ArrayList<>(1);
            list.add(from);
            toFromEdges.put(to, list);
        }
    }

    private void recalculateValidChanges() {
        undosValid = undosAll.stream()
                .filter(c -> isValidUndo.test(c.getChange()))
                .collect(Collectors.toCollection(ArrayList::new));
        redosValid = redosAll.stream()
                .filter(c -> isValidRedo.test(c.getChange()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public final boolean isQueuePositionValid(Source source, NonLinearChange<Source, C> savedChange)  {
        if (savedChange == null) {
            return undosAll.stream().noneMatch(changesDoneBy(source));
        }

        long revision = savedChange.getRevision();
        Predicate<NonLinearChange<Source, C>> sharesSameRevision = c -> c.getRevision() == revision;
        return undosAll.stream()
                .filter(changesDoneBy(source))
                .anyMatch(sharesSameRevision)
            || redosAll.stream()
                .filter(changesDoneBy(source))
                .anyMatch(sharesSameRevision);
    }

    public final NonLinearChange<Source, C> getLastChangeFor(Source source) {
        for (int i = undosAll.size() - 1; i >= 0; i--) {
            NonLinearChange<Source, C> change = undosAll.get(i);
            if (change.getSource() == source) {
                return change;
            }
        }
        return null;
    }

    private Predicate<NonLinearChange<Source, C>> changesDoneBy(Source source) {
        return c -> c.getSource() == source;
    }

    private List<NonLinearChange<Source, C>> getDependencies(NonLinearChange<Source, C> target) {
        return toFromEdges.containsKey(target)
                ? toFromEdges.get(target)
                : EMPTY_LIST;
    }

    private void remapAllEdges(NonLinearChange<Source, C> outdatedChange, NonLinearChange<Source, C> updatedChange) {
        if (!toFromEdges.isEmpty()) {
            for (NonLinearChange<Source, C> key : toFromEdges.keySet()) {
                if (key.equals(outdatedChange)) {
                    List<NonLinearChange<Source, C>> list = toFromEdges.remove(outdatedChange);
                    toFromEdges.put(updatedChange, list);
                } else {
                    List<NonLinearChange<Source, C>> list = toFromEdges.get(outdatedChange);
                    int index = list.indexOf(outdatedChange);
                    if (index >= 0) {
                        list.set(index, updatedChange);
                    }
                }
            }
        }
    }

    private List<C> extractChangesFrom(List<NonLinearChange<Source, C>> list) {
        List<C> extractedChanges = new ArrayList<>(list.size());
        list.forEach(c -> extractedChanges.add(c.getChange()));
        return extractedChanges;
    }

    public final boolean wasLastChangePerformedBy(Source source) {
        return undosAll.get(undosAll.size() - 1).getSource() == source;
    }

}
