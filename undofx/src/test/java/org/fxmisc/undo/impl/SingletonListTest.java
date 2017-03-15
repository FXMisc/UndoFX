package org.fxmisc.undo.impl;

import org.fxmisc.undo.impl.FixedSizeOverwritableList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SingletonListTest {

    private FixedSizeOverwritableList<String> list;
    private String aText = "a";
    private String bText = "b";

    @Before
    public void setup() {
        list = FixedSizeOverwritableList.withCapacity(1);
    }

    @Test
    public void constructedListIsInitiallyEmpty() {
        assertTrue(list.isEmpty());
    }

    @Test
    public void cannotRemoveItemFromEmptyList() {
        // when
        try {
            list.remove(0);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            // cannot remove when list is empty
        }
    }

    @Test
    public void addingWorks() {
        // when
        list.add(aText);

        // then
        assertEquals(aText, list.get(0));
    }

    @Test
    public void removingWorks() {
        // given: a singleton list with one change
        list.add(aText);

        // when: remove that change
        String result = list.remove(0);

        // then: list is empty
        assertTrue(list.isEmpty());
        assertEquals(aText, result);
    }

    @Test
    public void addingItemToFullListOverwritesChange() {
        // given
        list.add(aText);
        assertEquals(aText, list.get(0));

        // when
        list.add(bText);

        // then
        assertNotEquals(aText, list.get(0));
        assertEquals(bText, list.get(0));
    }

}

