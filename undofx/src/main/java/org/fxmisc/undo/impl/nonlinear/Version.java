package org.fxmisc.undo.impl.nonlinear;

/**
 * Used to determine whether a {@link org.fxmisc.undo.impl.ChangeQueue.QueuePosition} is still valid. The change can
 * be updated in light of a pushed, undone, or redone change without issue as the {@link #identity} object, not the
 * change object, is used to determine whether
 * {@link org.fxmisc.undo.impl.ChangeQueue.QueuePosition#isValid a queue position's validity} is still valid.
 *
 * @param <C> the immutable change object
 */
class Version<C> {

    private final Object identity;
    public final Object getIdentity() { return identity; }

    public final boolean identityEquals(Version<?> other) {
        return identity == other.identity;
    }

    private final int index;
    public final int getIndex() { return index; }

    private C change;
    public final C getChange() { return change; }
    public Version<C> setChange(C c) { return new Version<C>(identity, index, c); }

    public Version(int index, C c) {
        this(new Object(), index, c);
    }

    private Version(Object identity, int index, C change) {
        this.identity = identity;
        this.index = index;
        this.change = change;
    }

    @Override
    public String toString() {
        return String.format("Version(identity=%s index=%s change=%s", identity, index, change);
    }

}
