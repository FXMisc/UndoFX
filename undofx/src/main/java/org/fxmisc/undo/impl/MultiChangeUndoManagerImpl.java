package org.fxmisc.undo.impl;

import org.reactfx.EventStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link UndoManagerImpl} specified for a {@link List} of changes
 *
 * @param <C> the type of change to store in the list of changes to undo/redo
 */
public class MultiChangeUndoManagerImpl<C> extends UndoManagerImpl<List<C>> {

    public MultiChangeUndoManagerImpl(
            ChangeQueue<List<C>> queue,
            Function<? super C, ? extends C> changeInvert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> changeMerge,
            Predicate<C> changeIsIdentity,
            EventStream<List<C>> changeSource) {
        this(queue, changeInvert, apply, changeMerge, changeIsIdentity, changeSource, Duration.ZERO);
    }

    public MultiChangeUndoManagerImpl(
            ChangeQueue<List<C>> queue,
            Function<? super C, ? extends C> changeInvert,
            Consumer<List<C>> apply,
            BiFunction<C, C, Optional<C>> changeMerge,
            Predicate<C> changeIsIdentity,
            EventStream<List<C>> changeSource,
            Duration preventMergeDelay) {
        super(
                queue,
                list -> {
                    List<C> l = new ArrayList<>(list.size());
                    // invert the contents of the list
                    // and store them in reversed order
                    for (int i = list.size() - 1; i >= 0; i--) {
                        l.add(changeInvert.apply(list.get(i)));
                    }
                    return l;
                },
                apply,
                (list1, list2) -> {
                    // if one list is empty, return the other list
                    if (list1.size() == 0) {
                        return Optional.of(list2);
                    } else if (list2.size() == 0) {
                        return Optional.of(list1);
                    }

                    // if both are the same size and every corresponding element
                    // can be merged, return a list with all merged items.
                    // Otherwise, return Optional.empty()
                    if (list1.size() == list2.size()) {
                        List<C> mergeList = new ArrayList<>(list1.size());
                        for (int i = 0; i < list1.size(); i++) {
                            C item1 = list1.get(i);
                            C item2 = list2.get(i);
                            Optional<C> merge = changeMerge.apply(item1, item2);
                            if (merge.isPresent()) {
                                mergeList.add(merge.get());
                            } else {
                                return Optional.empty();
                            }
                        }
                        return Optional.of(mergeList);
                    } else {
                        return Optional.empty();
                    }
                },
                list -> list.stream().allMatch(changeIsIdentity),
                changeSource,
                preventMergeDelay
        );
    }

}