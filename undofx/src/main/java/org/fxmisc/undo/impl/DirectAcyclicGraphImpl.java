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

public class DirectAcyclicGraphImpl<Source extends NonLinearChangeQueue<C>, C> implements DirectedAcyclicUndoGraph<Source, C> {

    private final SuspendableList<NonLinearChange<Source, C>> allChanges = LiveList.suspendable(FXCollections.emptyObservableList());
    private final SuspendableList<NonLinearChange<Source, C>> validChanges = LiveList.suspendable(FXCollections.emptyObservableList());
    private final List<Edge<Source, C>> edges = new ArrayList<>();

    private final Suspendable listSuspender;
    private final SuspendableNo performingAction = new SuspendableNo();

    private final BiPredicate<C, C> isDependentOn;
    private final BiFunction<C, C, C> updater;
    private final BiFunction<C, List<C>, BubbledResult<C>> splitter;
    private final BiFunction<C, List<C>, List<C>> invalidChangesFinder;
    private final BiPredicate<C, List<C>> isBubblyChange;

    private long revision = 0;

    public DirectAcyclicGraphImpl(
            BiFunction<C, C, C> updater,
            BiPredicate<C, C> isDependentOn,
            BiFunction<C, List<C>, BubbledResult<C>> splitter,
            BiFunction<C, List<C>, List<C>> invalidChangesFinder,
            BiPredicate<C, List<C>> isBubblyChange) {
        this.updater = updater;
        this.isDependentOn = isDependentOn;
        this.splitter = splitter;
        this.invalidChangesFinder = invalidChangesFinder;
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

    public void forgetChanges(List<NonLinearChange<Source, C>> changes) {
        try (Guard commitOnClose = listSuspender.suspend()) {
            allChanges.removeAll(changes);
            validChanges.removeAll(changes);
            changes.forEach(this::removeRelatedEdgesOf);
        }
    }

    public final NonLinearChange<Source, C> createChange(Source source, C change) {
        return new NonLinearChange<>(source, change, revision++);
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
            List<NonLinearChange<Source, C>> dependencies = new ArrayList<>();
            edges.stream()
                    .filter(e -> e.getTo().equals(dep))
                    .map(Edge::getFrom)
                    .forEach(dependencies::add);

            boolean isMutuallyIndependent = dependencies.isEmpty();
            if (isMutuallyIndependent ||
                    isBubblyChange.test(dep.getChange(), extractChangesFrom(dependencies))) {
                if (!validChanges.contains(dep)) {
                    validChanges.add(dep);
                }
            }
        });
    }

    public final void push(List<NonLinearChange<Source, C>> redoList,
                           List<NonLinearChange<Source, C>> newChanges) {
        try (Guard commitOnClose = listSuspender.suspend()) {
            redoList.forEach(redo -> {
                allChanges.remove(redo);
                validChanges.remove(redo);
                removeRelatedEdgesOf(redo);
            });

            newChanges.forEach(addedChange -> {
                if (!allChanges.isEmpty()) {
                    for (int i = 0; i < allChanges.size(); i++) {
                        NonLinearChange<Source, C> oldNLChange = allChanges.get(i);
                        C updatedChange = updater.apply(addedChange.getChange(), oldNLChange.getChange());
                        NonLinearChange<Source, C> newNLChange = oldNLChange.updateChange(updatedChange);

                        if (!oldNLChange.equals(newNLChange)) {
                            allChanges.set(i, newNLChange);
                            int index = validChanges.indexOf(oldNLChange);
                            validChanges.set(index, newNLChange);

                            remapAllEdges(oldNLChange, newNLChange);
                        }

                        if (isDependentOn.test(addedChange.getChange(), newNLChange.getChange())) {
                            edges.add(new Edge<>(addedChange, newNLChange));
                        }
                    }

                    List<C> changesToRemove = invalidChangesFinder.apply(addedChange.getChange(), extractChangesFrom(validChanges));
                    if (!changesToRemove.isEmpty()) {
                        validChanges.stream()
                                .filter(c -> changesToRemove.contains(c.getChange()))
                                .forEach(validChanges::remove);
                    }

                }

                allChanges.add(addedChange);
                validChanges.add(addedChange);
            });
        }
    }

    private void removeRelatedEdgesOf(NonLinearChange<Source, C> target) {
        edges.stream()
                .filter(e -> e.getFrom().equals(target) || e.getTo().equals(target))
                .forEach(edges::remove);
    }

    // TODO: This name should be changed [independent / dependent, to / from, target / (reliant, chooser, user, changer)] ?
    private List<NonLinearChange<Source, C>> getDependencies(NonLinearChange<Source, C> target) {
        List<NonLinearChange<Source, C>> dependencies = new ArrayList<>();
        edges.stream()
                .filter(e -> e.getTo().equals(target))
                .forEach(e -> dependencies.add(e.getFrom()));
        return dependencies;
    }

    private List<NonLinearChange<Source, C>> getDependents(NonLinearChange<Source, C> target) {
        List<NonLinearChange<Source, C>> dependents = new ArrayList<>();
        edges.stream()
                .filter(e -> e.getFrom().equals(target))
                .forEach(e -> dependents.add(e.getTo()));
        return dependents;
    }

    private void remapAllEdges(NonLinearChange<Source, C> oldChange, NonLinearChange<Source, C> newChange) {
        if (!edges.isEmpty()) {
            for (int i = 0; i < edges.size(); i++) {
                Edge<Source, C> edge = edges.get(i);

                NonLinearChange<Source, C> from = edge.getFrom();
                if (oldChange.equals(from)) {
                    edge = edge.updateFrom(newChange);
                }

                NonLinearChange<Source, C> to = edge.getTo();
                if (oldChange.equals(to)) {
                    edge = edge.updateTo(newChange);
                }

                edges.set(i , edge);
            }
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
