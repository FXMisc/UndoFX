package org.fxmisc.undo.impl;

public class BubbledResult<C> {

    private final C bubbled;
    private final C buried;

    public BubbledResult(C bubbled, C buried) {
        this.bubbled = bubbled;
        this.buried = buried;
    }

    public final C getBubbled() { return bubbled; }
    public final C getBuried() { return buried; }

}
