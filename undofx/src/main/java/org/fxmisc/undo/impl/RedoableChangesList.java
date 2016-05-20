package org.fxmisc.undo.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RedoableChangesList<Source extends NonLinearChangeQueue<C>, C> {

    private final Predicate<C> validator;
    private final BiFunction<C, C, C> updater;
    private final Function<C, BubbledResult<C>> bubbler;

    private final List<C> all = new ArrayList<>(1);
    private List<C> valid = new ArrayList<>(1);

    private final Source source;
    public final Source getSource() { return source; }

    public RedoableChangesList(Source source, BiFunction<C, C, C> updater, Predicate<C> validator,
                               Function<C, BubbledResult<C>> bubbler) {
        this.source = source;
        this.updater = updater;
        this.validator = validator;
        this.bubbler = bubbler;
    }

    public final void updateRedos(C newChange) {
        for (int i = 0; i < all.size(); i++) {
            C outdatedChange = all.get(i);
            C updatedChange = updater.apply(newChange, outdatedChange);
            all.set(i, updatedChange);
        }
    }

    public final void recheckValidity() {
        valid = all.stream().filter(validator).collect(Collectors.toCollection(ArrayList::new));
    }

    public final void addChange(C change) {
        all.add(change);
        recheckValidity();
    }

    public final C getLastValidChange() {
        C change = valid.remove(valid.size() - 1);
        BubbledResult<C> result = bubbler.apply(change);
        if (result.getBuried() == null) {
            all.remove(change);
            return change;
        } else {
            int index = all.indexOf(change);
            all.set(index, result.getBuried());
            return result.getBubbled();
        }
    }

    public final boolean isEmpty() {
        return valid.isEmpty();
    }

    public final void clear() {
        valid.clear();
        all.clear();
    }

}
