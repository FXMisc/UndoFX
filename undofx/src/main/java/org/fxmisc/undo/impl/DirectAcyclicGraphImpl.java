package org.fxmisc.undo.impl;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import org.fxmisc.undo.DirectedAcyclicUndoGraph;
import org.reactfx.Guard;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.SuspendableList;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

public class DirectAcyclicGraphImpl<Source extends NonLinearChangeQueue<C>, C> implements DirectedAcyclicUndoGraph<Source, C> {

    private final List<NonLinearChange<Source, C>> EMPTY_LIST = new ArrayList<>(0);

    private final SuspendableList<NonLinearChange<Source, C>> allChanges = LiveList.suspendable(FXCollections.emptyObservableList());
    private final SuspendableList<NonLinearChange<Source, C>> validChanges = LiveList.suspendable(FXCollections.emptyObservableList());

    private final HashMap<Source, ArrayList<C>> redoableChangesLists = new HashMap<>();

    private final HashMap<NonLinearChange<Source, C>, ArrayList<NonLinearChange<Source, C>>> fromToEdges = new HashMap<>();
    private final HashMap<NonLinearChange<Source, C>, ArrayList<NonLinearChange<Source, C>>> toFromEdges = new HashMap<>();

    private final Suspendable listSuspender;
    private final SuspendableNo performingAction = new SuspendableNo();

    private final BiPredicate<C, C> isDependentOn;
    private final BiFunction<C, C, C> undoUpdater;
    private final BiFunction<C, C, C> redoUpdater;
    private final BiFunction<C, List<C>, BubbledResult<C>> splitter;
    private final BiFunction<C, List<C>, List<C>> findInvalidChanges;
    private final BiPredicate<C, List<C>> isBubblyChange;

    private long revision = 0;

    public DirectAcyclicGraphImpl(
            BiFunction<C, C, C> undoUpdater,
            BiFunction<C, C, C> redoUpdater,
            BiPredicate<C, C> isDependentOn,
            BiFunction<C, List<C>, BubbledResult<C>> splitter,
            BiFunction<C, List<C>, List<C>> findInvalidChanges,
            BiPredicate<C, List<C>> isBubblyChange) {
        this.undoUpdater = undoUpdater;
        this.redoUpdater = redoUpdater;
        this.isDependentOn = isDependentOn;
        this.splitter = splitter;
        this.findInvalidChanges = findInvalidChanges;
        this.isBubblyChange = isBubblyChange;
        this.listSuspender = Suspendable.combine(
                // unsuspend validChanges second to calculate queue's currentPosition
                validChanges,

                // unsuspend allChanges first
                allChanges
        );
    }

    public final FilteredList<NonLinearChange<Source, C>> allChangesFor(Source source) {
        return allChanges.filtered(c -> c.getSource().equals(source));
    }

    public final FilteredList<NonLinearChange<Source, C>> validChangesFor(Source source) {
        return validChanges.filtered(c -> c.getSource().equals(source));
    }

    public final boolean hasNextFor(Source source) {
        return !getRedoListFor(source).isEmpty();
    }

    public final C nextFor(Source source) {
        List<C> redos = getRedoListFor(source);
        return redos.remove(redos.size() - 1);
    }

    public final void addRedoableChange(Source source, C change) {
        getRedoListFor(source).add(change);
    }

    private List<C> getRedoListFor(Source source) {
        return redoableChangesLists.get(source);
    }

    public void addRedoableListFor(Source source) {
        redoableChangesLists.put(source, new ArrayList<>());
    }

    public void closeDown(Source source) {
        redoableChangesLists.remove(source);
        Set<Source> srcSet = redoableChangesLists.keySet();
        List<NonLinearChange<Source, C>> changes = new ArrayList<>();
        validChanges.forEach(vc -> {
            List<NonLinearChange<Source, C>> dependents = getDependents(vc);
            // TODO: remove all unneeded changes from closing source
            // The issue: no reference of the content being changed is available. So only way we know
            // of its existence is through
            /*
                if item is mutually independent (not bubbleable)
                    if item's source == closing source
                        remove that item
                    otherwise
                        look at its dependencies....

                given dependency,
             */
            boolean isMutuallyIndependent = dependents.isEmpty();
            if (source.equals(vc.getSource())) {
                if (isMutuallyIndependent) {
                    removeRelatedEdgesOf(vc);
                }
            }
            if (isMutuallyIndependent) {
            } else {

            }
        });
        // TODO: recursive method that removes 2nd-level source edges
    }

    public void forgetChanges(List<NonLinearChange<Source, C>> changes) {
        try (Guard commitOnClose = listSuspender.suspend()) {
            allChanges.removeAll(changes);
            validChanges.removeAll(changes);
            changes.forEach(this::removeRelatedEdgesOf);
        }
    }

    public final NonLinearChange<Source, C> getValidChange(NonLinearChange<Source, C> nonLinearChange) {
        List<NonLinearChange<Source, C>> dependencies = getDependencies(nonLinearChange);
        if (dependencies.size() == 0) {
            try (Guard commitOnClose = validChanges.suspend()) {
                validChanges.remove(nonLinearChange);

                List<NonLinearChange<Source, C>> dependents = getDependents(nonLinearChange);
                removeRelatedEdgesOf(nonLinearChange);
                recalculateValidChanges(dependents);
            }

            return nonLinearChange;
        } else {
            NonLinearChange<Source, C> bubbledChange;
            try (Guard commitOnClose = allChanges.suspend()) {
                List<C> changes = extractChangesFrom(dependencies);
                BubbledResult<C> result = splitter.apply(nonLinearChange.getChange(), changes);

                NonLinearChange<Source, C> buriedChange = nonLinearChange.updateChange(result.getBuried());
                int index = allChanges.indexOf(nonLinearChange);
                allChanges.set(index, buriedChange);
                remapAllEdges(nonLinearChange, buriedChange);

                bubbledChange = new NonLinearChange<>(
                        nonLinearChange.getSource(),
                        result.getBubbled(),
                        revision++
                );
                allChanges.add(bubbledChange);
            }

            return bubbledChange;
        }
    }

    private void recalculateValidChanges(List<NonLinearChange<Source, C>> dependents) {
        dependents.forEach(dep -> {
            List<NonLinearChange<Source, C>> dependencies = getDependencies(dep);

            boolean isMutuallyIndependent = dependencies.isEmpty();
            if (isMutuallyIndependent ||
                isBubblyChange.test(dep.getChange(), extractChangesFrom(dependencies))
            ) {
                if (!validChanges.contains(dep)) {
                    validChanges.add(dep);
                }
            }
        });
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
        try (Guard commitOnClose = listSuspender.suspend()) {
            Stream.of(newChanges).forEach(c -> {
                updateRedoableChanges(c);
                updateUndoableChanges(source, c);
            });
        }
    }

    private void updateRedoableChanges(C change) {
        for (Source key : redoableChangesLists.keySet()) {
            ArrayList<C> redoList = redoableChangesLists.get(key);
            remapList(redoList, outdatedRedoChange -> redoUpdater.apply(change, outdatedRedoChange));
            redoableChangesLists.replace(key, redoList);
        }
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
                    if (validChanges.contains(outdatedChange)) {
                        int index = validChanges.indexOf(outdatedChange);
                        validChanges.set(index, updatedChange);
                    }

                    remapAllEdges(outdatedChange, updatedChange);
                } else {
                    updatedChange = outdatedChange;
                }

                if (isDependentOn.test(change, updated)) {
                    addEdge(addedChange, updatedChange);
                }
            }

            List<C> changesToRemove = findInvalidChanges.apply(change, extractChangesFrom(validChanges));
            if (!changesToRemove.isEmpty()) {
                validChanges.stream()
                        .filter(vc -> changesToRemove.contains(vc.getChange()))
                        .forEach(validChanges::remove);
            }

        }

        allChanges.add(addedChange);
        validChanges.add(addedChange);
    }

    private void removeRelatedEdgesOf(NonLinearChange<Source, C> target) {
        List<NonLinearChange<Source, C>> list = fromToEdges.remove(target);
        list.forEach(c -> toFromEdges.get(c).remove(target));
    }

    private void addEdge(NonLinearChange<Source, C> from, NonLinearChange<Source, C> to) {
        if (fromToEdges.containsKey(from)) {
            fromToEdges.get(from).add(to);
        } else {
            ArrayList<NonLinearChange<Source, C>> list = new ArrayList<>(1);
            list.add(to);
            fromToEdges.put(from, list);
        }

        if (toFromEdges.containsKey(to)) {
            toFromEdges.get(to).add(from);
        } else {
            ArrayList<NonLinearChange<Source, C>> list = new ArrayList<>(1);
            list.add(from);
            fromToEdges.put(to, list);
        }
    }

    // TODO: This name should be changed [independent / dependent, to / from, target / (reliant, chooser, user, changer)] ?
    private List<NonLinearChange<Source, C>> getDependencies(NonLinearChange<Source, C> target) {
        return toFromEdges.containsKey(target)
                ? toFromEdges.get(target)
                : EMPTY_LIST;
    }

    private List<NonLinearChange<Source, C>> getDependents(NonLinearChange<Source, C> target) {
        return fromToEdges.containsKey(target)
                ? fromToEdges.get(target)
                : EMPTY_LIST;
    }

    private void remapAllEdges(NonLinearChange<Source, C> outdatedChange, NonLinearChange<Source, C> updatedChange) {
        if (!fromToEdges.isEmpty() && fromToEdges.containsKey(outdatedChange)) {
            ArrayList<NonLinearChange<Source, C>> list = fromToEdges.remove(outdatedChange);
            fromToEdges.put(updatedChange, list);

            list.forEach(c -> {
                ArrayList<NonLinearChange<Source, C>> ls = toFromEdges.get(c);
                ls.remove(outdatedChange);
                ls.add(updatedChange);
            });
        }
    }

    private <T> void remapList(List<T> list, Function<T, T> updater) {
        for (int i = 0; i < list.size(); i++) {
            T item = list.get(i);
            T updatedItem = updater.apply(item);
            list.set(i, updatedItem);
        }
    }

    private List<C> extractChangesFrom(List<NonLinearChange<Source, C>> list) {
        List<C> extractedChanges = new ArrayList<>(list.size());
        list.forEach(c -> extractedChanges.add(c.getChange()));
        return extractedChanges;
    }

    public SuspendableNo performingActionProperty() {
        return performingAction;
    }

    public Source getLastChangeSource() {
        return allChanges.get(allChanges.size() - 1).getSource();
    }

}
