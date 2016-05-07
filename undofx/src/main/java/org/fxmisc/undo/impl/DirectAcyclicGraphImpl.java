package org.fxmisc.undo.impl;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.fxmisc.undo.DirectedAcyclicUndoGraph;
import org.reactfx.SuspendableNo;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class DirectAcyclicGraphImpl<Source extends ChangeQueue<C>, C> implements DirectedAcyclicUndoGraph<Source, C> {

    // TODO: should changes' type be a SuspendableList ?
    private final ObservableList<NonLinearChange<Source, C>> changes = FXCollections.emptyObservableList();
    private final HashMap<NonLinearChange<Source, C>, List<NonLinearChange<Source, C>>> edges = new HashMap<>();
    private final SuspendableNo performingAction = new SuspendableNo();

    private final BiPredicate<C, C> dependencyFinder;
    private final BiFunction<C, C, C> updater;
    private final BiFunction<C, List<C>, BubbledResult<C>> splitter;

    public DirectAcyclicGraphImpl(
            BiFunction<C, C, C> updater,
            BiPredicate<C, C> dependencyFinder,
            BiFunction<C, List<C>, BubbledResult<C>> splitter) {
        this.updater = updater;
        this.dependencyFinder = dependencyFinder;
        this.splitter = splitter;
    }

    public final FilteredList<NonLinearChange<Source, C>> getChangesFor(Source source) {
        return changes.filtered(c -> c.getSource().equals(source));
    }

    public void forgetChanges(List<NonLinearChange<Source, C>> changes) {
        this.changes.removeAll(changes);
        changes.forEach(edges::remove);
    }

    public NonLinearChange<Source, C> getValidChangeFor(NonLinearChange<Source, C> nonLinearChange, Supplier<Long> bubbledRevision) {
        List<NonLinearChange<Source, C>> dependencies = edges.remove(nonLinearChange);
        if (dependencies.size() == 0) {
            changes.remove(nonLinearChange);
            return nonLinearChange;
        } else {
            List<C> changes = new ArrayList<>(dependencies.size());
            dependencies.forEach(dep -> changes.add(dep.getChange()));
            BubbledResult<C> result = splitter.apply(nonLinearChange.getChange(), changes);

            NonLinearChange<Source, C> buriedChange = nonLinearChange.updateChange(result.getBuried());
            int index = changes.indexOf(nonLinearChange);
            this.changes.set(index, buriedChange);
            edges.put(buriedChange, dependencies);
            edges.keySet().forEach(key -> {
                if (!key.equals(nonLinearChange)) {
                    remapSingleDependency(nonLinearChange, buriedChange);
                }
            });

            // TODO: need to update the NonLinear-ChangeQueue's changes list here

            return new NonLinearChange<>(
                    nonLinearChange.getSource(),
                    result.getBubbled(),
                    bubbledRevision.get()
            );
        }
    }

    private void remapSingleDependency(NonLinearChange<Source, C> original, NonLinearChange<Source, C> updated) {
        List<NonLinearChange<Source, C>> dependencies = edges.get(original);
        if (!dependencies.isEmpty()) {
            for (int i = 0; i < dependencies.size(); i++) {
                if (dependencies.get(i).equals(original)) {
                    dependencies.set(i, updated);
                }
            }
        }
    }

    public final void push(List<NonLinearChange<Source, C>> redoList,
                           List<NonLinearChange<Source, C>> newChanges) {
        changes.removeAll(redoList);

        HashMap<NonLinearChange<Source, C>, NonLinearChange<Source, C>> oldToNewChanges = new HashMap<>();

        newChanges.forEach(addedChange -> {
            for (int i = 0; i < changes.size(); i++) {
                NonLinearChange<Source, C> oldNLChange = changes.get(i);
                C updatedChange = updater.apply(addedChange.getChange(), oldNLChange.getChange());
                NonLinearChange<Source, C> newNLChange = oldNLChange.updateChange(updatedChange);

                if (!oldNLChange.equals(newNLChange)) {
                    changes.set(i, newNLChange);
                    oldToNewChanges.put(oldNLChange, newNLChange);
                }

                List<NonLinearChange<Source, C>> dependencies = edges.get(oldNLChange);
                if (dependencyFinder.test(newNLChange.getChange(), addedChange.getChange())) {
                    dependencies.add(addedChange);
                }
            }
            changes.add(addedChange);
            edges.put(addedChange, new ArrayList<>());
        });

        remapAllEdges(oldToNewChanges);
    }

    private void remapAllEdges(Map<NonLinearChange<Source, C>, NonLinearChange<Source, C>> oldToNewChanges) {
        for (NonLinearChange<Source, C> key : edges.keySet()) {
            if (oldToNewChanges.containsKey(key)) {
                List<NonLinearChange<Source, C>> dependencies = edges.remove(key);
                remapMultipleDependencies(dependencies, oldToNewChanges);
                edges.put(oldToNewChanges.get(key), dependencies);
            } else {
                List<NonLinearChange<Source, C>> dependencies = edges.get(key);
                remapMultipleDependencies(dependencies, oldToNewChanges);
            }
        }
    }

    private void remapMultipleDependencies(List<NonLinearChange<Source, C>> dependencies,
                                           Map<NonLinearChange<Source, C>, NonLinearChange<Source, C>> oldToNewChanges) {
        if (!dependencies.isEmpty()) {
            for (int i = 0; i < dependencies.size(); i++) {
                NonLinearChange<Source, C> oldDependency = dependencies.get(i);
                if (oldToNewChanges.containsKey(oldDependency)) {
                    dependencies.set(i, oldToNewChanges.get(oldDependency));
                }
            }
        }

    }

    public SuspendableNo performingActionProperty() {
        return performingAction;
    }

    public Source getLastChangeSource() {
        return changes.get(changes.size() - 1).getSource();
    }

}
