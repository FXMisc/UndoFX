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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DirectAcyclicGraphImpl<Source extends NonLinearChangeQueue<C>, C> implements DirectedAcyclicUndoGraph<Source, C> {

    private final List<NonLinearChange<Source, C>> EMPTY_LIST = new ArrayList<>(0);

    private final List<RedoableChangesList<Source, C>> redoableChangesLists = new ArrayList<>(1);
    private final List<NonLinearChange<Source, C>> allChanges = new ArrayList<>();
    private List<NonLinearChange<Source, C>> validChanges = new ArrayList<>();

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

    private long revision = 0;

    public DirectAcyclicGraphImpl(
            BiPredicate<C, C> firstDependsOnSecond,
            BiFunction<C, C, C> undoUpdater,
            Predicate<C> isValidUndo,
            BiFunction<C, List<C>, BubbledResult<C>> undoBubbler,
            TriFunction<C, C, BubbledResult<C>, C> postBubbleUndoUpdater,
            BiFunction<C, C, C> redoUpdater,
            Predicate<C> isValidRedo,
            Function<C, BubbledResult<C>> redoBubbler) {
        this.firstDependsOnSecond = firstDependsOnSecond;

        this.undoUpdater = undoUpdater;
        this.isValidUndo = isValidUndo;
        this.undoBubbler = undoBubbler;
        this.postBubbleUndoUpdater = postBubbleUndoUpdater;

        this.redoUpdater = redoUpdater;
        this.isValidRedo = isValidRedo;
        this.redoBubbler = redoBubbler;
    }

    public final boolean hasNextFor(Source source) {
        return !getRedoListFor(source).isEmpty();
    }

    public final C nextFor(Source source) {
        return getRedoListFor(source).getLastValidChange();
    }

    public final boolean hasPrevFor(Source source) {
        return validChanges.stream().anyMatch(c -> c.getSource() == source);
    };

    public final C prevFor(Source source) {
        for (int i = validChanges.size() - 1; i >= 0; i--) {
            NonLinearChange<Source, C> change = validChanges.get(i);
            if (change.getSource() == source) {
                return getValidFormOf(change).getChange();
            }
        }
        throw new IllegalStateException("Unreachable code: Method 'prevFor(Source)' should only be called when" +
                "method 'hasPrevFor(Source)' returns true");
    }

    public final void addRedoableChangeFor(Source source, C change) {
        getRedoListFor(source).addChange(change);
    }

    private RedoableChangesList<Source, C> getRedoListFor(Source source) {
        for (RedoableChangesList<Source, C> list : redoableChangesLists) {
            if (list.getSource() == source) {
                return list;
            }
        }
        throw new IllegalStateException("Unreachable Code: Attempted to get the redo list for a NonLinearChangeQueue" +
                "that hasn't been registered for this graph. Source: " + source.toString());
    }

    public void registerRedoableListFor(Source source) {
        redoableChangesLists.add(new RedoableChangesList<>(source, redoUpdater, isValidRedo, redoBubbler));
    }

    public void closeDown(Source source) {
        redoableChangesLists.removeIf(l -> l.getSource() == source);
        allChanges.stream()
                .filter(c -> c.getSource() == source)
                .forEach(c -> {
            removeRelatedEdgesOf(c);
            allChanges.remove(c);
        });
    }

    // TODO: all history from start -> current position should be forgotten, not next current valid change
    public void forgetHistoryFor(Source source, Consumer<Integer> ifForgotten) {
        if (hasPrevFor(source)) {
            List<NonLinearChange<Source, C>> history = allChanges.stream()
                    .filter(c -> c.getSource() == source)
                    .collect(Collectors.toCollection(ArrayList::new));

            NonLinearChange<Source, C> lastValidChange = null;
            for (int i = validChanges.size() - 1; i >= 0; i--) {
                lastValidChange = validChanges.get(i);
                if (lastValidChange.getSource() == source) {
                    break;
                }
            }

            int index = history.indexOf(lastValidChange);
            List<NonLinearChange<Source, C>> forgotten = history.subList(0, index);
            forgotten.forEach(c -> {
                removeRelatedEdgesOf(c);
                allChanges.remove(c);
            });

            ifForgotten.accept(forgotten.size());
        }
    }

    public final NonLinearChange<Source, C> getValidFormOf(NonLinearChange<Source, C> nonLinearChange) {
        List<NonLinearChange<Source, C>> dependencies = getDependencies(nonLinearChange);
        boolean isMutuallyIndependent = dependencies.isEmpty();
        if (isMutuallyIndependent) {
            removeRelatedEdgesOf(nonLinearChange);

            return nonLinearChange;
        } else {
            List<C> dependencyChanges = extractChangesFrom(dependencies);
            C original = nonLinearChange.getChange();
            BubbledResult<C> bubbleResult = undoBubbler.apply(original, dependencyChanges);

            updateUndosPostBubble(nonLinearChange, original, bubbleResult);

            NonLinearChange<Source, C> buriedChange = nonLinearChange.updateChange(bubbleResult.getBuried());
            int index = allChanges.indexOf(nonLinearChange);
            allChanges.set(index, buriedChange);
            remapAllEdges(nonLinearChange, buriedChange);

            return new NonLinearChange<>(nonLinearChange.getSource(), bubbleResult.getBubbled(), revision++);
        }
    }

    private void updateUndosPostBubble(NonLinearChange<Source, C> change, C original, BubbledResult<C> bubbleResult) {
        for (int i = 0; i < allChanges.size(); i++) {
            NonLinearChange<Source, C> outdatedChange = allChanges.get(i);
            if (!change.equals(outdatedChange)) {
                C outdated = outdatedChange.getChange();
                C updated = postBubbleUndoUpdater.apply(outdated, original, bubbleResult);
                if (!outdated.equals(updated)) {
                    NonLinearChange<Source, C> updatedChange = outdatedChange.updateChange(updated);
                    allChanges.set(i, updatedChange);
                    remapAllEdges(outdatedChange, updatedChange);
                }
            }
        }
    }

    @SafeVarargs
    public final void pushChanges(Source source, C... newChanges) {
        getRedoListFor(source).clear();

        push(source, newChanges);
    }

    public final void pushRedo(Source source, C redoChange) {
        push(source, redoChange);
    }

    @SafeVarargs
    private final void push(Source source, C... newChanges) {
        Stream.of(newChanges).forEach(c -> {
            updateRedoableChanges(c);
            updateUndoableChanges(source, c);
        });
        recalculateValidUndos();
    }

    private void updateRedoableChanges(C change) {
        redoableChangesLists.forEach(list-> {
            list.updateRedos(change);
            list.recheckValidity();
        });
    }

    private void updateUndoableChanges(Source source, C change) {
        NonLinearChange<Source, C> addedChange = new NonLinearChange<>(source, change, revision++);

        if (!allChanges.isEmpty()) {
            for (int i = 0; i < allChanges.size(); i++) {
                NonLinearChange<Source, C> outdatedChange = allChanges.get(i);
                C outdated = outdatedChange.getChange();
                C updated = undoUpdater.apply(change, outdated);

                NonLinearChange<Source, C> updatedChange;
                if (!outdated.equals(updated)) {
                    updatedChange = outdatedChange.updateChange(updated);
                    allChanges.set(i, updatedChange);
                    remapAllEdges(outdatedChange, updatedChange);

                    int index = validChanges.indexOf(outdatedChange);
                    if (index >= 0) {
                        validChanges.set(index, updatedChange);
                    }
                } else {
                    updatedChange = outdatedChange;
                }

                if (firstDependsOnSecond.test(change, updated)) {
                    addEdgeFromTo(addedChange, updatedChange);
                }
            }
        }

        allChanges.add(addedChange);
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

    private void recalculateValidUndos() {
        validChanges = allChanges.stream()
                .filter(c -> isValidUndo.test(c.getChange()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public final boolean isQueuePositionValid(Source source, NonLinearChange<Source, C> savedChange)  {
        if (savedChange == null) {
            return allChanges.stream().noneMatch(c -> c.getSource() == source);
        }

        C change = savedChange.getChange();
        long revision = savedChange.getRevision();
        return allChanges.stream()
                .filter(c -> c.getSource() == source)
                .anyMatch(c -> c.getRevision() == revision)
                || getRedoListFor(source).contains(change);
    }

    public final NonLinearChange<Source, C> getLastChangeFor(Source source) {
        NonLinearChange<Source, C> lastChange = null;
        for (int i = allChanges.size() - 1; i >= 0; i--) {
            NonLinearChange<Source, C> change = allChanges.get(i);
            if (change.getSource() == source) {
                lastChange = change;
                break;
            }
        }
        return lastChange;
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
        return allChanges.get(allChanges.size() - 1).getSource() == source;
    }

}
