package org.fxmisc.richtextfx;

public enum UndoBubbleType {
    /** Indicates the left side of the text change was grounded and the right side is bubbled */
    GROUNDED_LEFT_BUBBLY_RIGHT,
    /** Indicates the middle of the text change was bubbled and the left and right sides were combined into grounded */
    BUBBLY_MIDDLE_GROUNDED_LEFT_PLUS_RIGHT,
    /** Indicates the left side of the text change was bubbled and the right side is grounded */
    BUBBLY_LEFT_GROUNDED_RIGHT
}
