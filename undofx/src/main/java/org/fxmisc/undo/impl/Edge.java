package org.fxmisc.undo.impl;

/**
 * Created by jordan on 5/7/16.
 */
public class Edge<S extends NonLinearChangeQueue<C>, C> {

    private final NonLinearChange<S, C> from;
    private final NonLinearChange<S, C> to;

    public Edge(NonLinearChange<S, C> from, NonLinearChange<S, C> to) {
        this.from = from;
        this.to = to;
    }

    public final NonLinearChange<S, C> getFrom() { return from; }
    public final NonLinearChange<S, C> getTo() { return to; }

    public final Edge<S, C> updateFrom(NonLinearChange<S, C> from) {
        return new Edge<>(from, to);
    }

    public final Edge<S, C> updateTo(NonLinearChange<S, C> to) {
        return new Edge<>(from, to);
    }

    @Override
    public String toString() {
        return "Edge(" + from.toString() + " --> " + to.toString() + ")";
    }
}
