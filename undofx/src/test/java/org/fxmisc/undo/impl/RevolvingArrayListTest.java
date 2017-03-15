package org.fxmisc.undo.impl;

import com.nitorcreations.junit.runners.NestedRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(NestedRunner.class)
public class RevolvingArrayListTest {

    private static void FOR(int times, Runnable DO) {
        for (int i = 1; i < times + 1; i++) {
            DO.run();
        }
    }

    private final String firstValue = "first";
    private final String value = "value";
    private final String insertion = "insertion";

    public class WhenCapacityIsGreaterThanTwo {

        private final int capacity = 10;
        private final int halfCapacity = capacity / 2;
        private final int halfInsertionIndex = halfCapacity - 2;

        private List<String> list =  FixedSizeOverwritableList.withCapacity(capacity);

        public class AndZeroIndexIsZero {

            public class AndListIsEmpty {

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void changeRemoveDoesNotWork() {
                    assertFalse(list.remove("some string"));
                }

                @Test
                public void indexRemoveDoesNotWork() {
                    try {
                        list.remove(0);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // 0 >= list.size()
                    }
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    try {
                        list.remove(capacity - 1);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // 0 >= list.size()
                    }
                }

            }

            public class AndListIsHalfFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    FOR(halfCapacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(halfInsertionIndex, insertion);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, halfInsertionIndex));
                    assertEquals(insertion, list.get(halfInsertionIndex));
                    assertAllMatchValue(list.subList(halfInsertionIndex + 1, halfCapacity));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveWorks() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

            }

            public class AndListIsFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    FOR(capacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddOverwritesZeroIndex() {
                    list.add(insertion);

                    assertAllMatchValue(list.subList(0, capacity - 2));
                    assertEquals(insertion, list.get(capacity - 1));
                }

                @Test
                public void indexAndChangeAddOverwritesZeroIndex() {
                    list.add(capacity - 1, insertion);

                    assertAllMatchValue(list.subList(0, capacity - 2));
                    assertEquals(insertion, list.get(capacity - 1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }
            }
        }

        public class AndZeroIndexIsMiddle {

            @Before
            public void setup() {
                FOR(capacity + halfCapacity, () -> list.add(value));
                FOR(capacity, () -> list.remove(0));
            }

            public class AndListIsEmpty {

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void changeRemoveDoesNotWork() {
                    assertFalse(list.remove("some string"));
                }

                @Test
                public void indexRemoveDoesNotWork() {
                    try {
                        list.remove(0);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // cannot remove when list is empty
                    }
                }

            }

            public class AndListIsHalfFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    FOR(halfCapacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, halfCapacity));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(halfInsertionIndex, insertion);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, halfInsertionIndex));
                    assertEquals(insertion, list.get(halfInsertionIndex));
                    assertAllMatchValue(list.subList(halfInsertionIndex + 1, halfCapacity));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

            }

            public class AndListIsFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    FOR(capacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddOverwritesZeroIndex() {
                    list.add(insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 1));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddOverwritesZeroIndex() {
                    list.add(capacity - 1, insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

            }

        }

        public class AndZeroIndexIsLastIndex {

            @Before
            public void setup() {
                FOR(capacity + capacity - 1, () -> list.add(value));
            }

            public class AndListIsEmpty {

                @Before
                public void setup() {
                    FOR(list.size(), () -> list.remove(0));
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void changeRemoveDoesNotWork() {
                    assertFalse(list.remove("some string"));
                }

                @Test
                public void indexRemoveDoesNotWork() {
                    try {
                        list.remove(0);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // cannot remove when list is empty
                    }
                }

            }

            public class AndListIsHalfFull {

                @Before
                public void setup() {
                    FOR(list.size(), () -> list.remove(0));

                    list.add(firstValue);
                    FOR(halfCapacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(halfInsertionIndex, insertion);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, halfInsertionIndex));
                    assertEquals(insertion, list.get(halfInsertionIndex));
                    assertAllMatchValue(list.subList(halfInsertionIndex + 1, halfCapacity));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

            }

            public class AndListIsFull {

                @Before
                public void setup() {
                    FOR(list.size(), () -> list.remove(0));

                    list.add(firstValue);
                    FOR(capacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddOverwritesZeroIndex() {
                    list.add(insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddOverwritesZeroIndex() {
                    list.add(list.size() - 1, insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveWorks() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }
            }
        }

    }

    public class WhenCapacityIsTwo {

        private final int capacity = 2;
        private final int halfCapacity = 1;
        private final int halfInsertionIndex = 0;

        private List<String> list =  FixedSizeOverwritableList.withCapacity(capacity);

        public class AndZeroIndexIsZero {

            public class AndListIsEmpty {

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void changeRemoveDoesNotWork() {
                    assertFalse(list.remove("some string"));
                }

                @Test
                public void indexRemoveDoesNotWork() {
                    try {
                        list.remove(0);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // 0 >= list.size()
                    }
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    try {
                        list.remove(capacity - 1);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // 0 >= list.size()
                    }
                }

            }

            public class AndListIsHalfFull {

                @Before
                public void setup() {
                    list.add(value);
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(value, list.get(0));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                    assertEquals(value, list.get(1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(0);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveWorks() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

            }

            public class AndListIsFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    list.add(value);
                }

                @Test
                public void changeAddOverwritesZeroIndex() {
                    list.add(insertion);

                    assertEquals(value, list.get(0));
                    assertEquals(insertion, list.get(capacity - 1));
                }

                @Test
                public void indexAndChangeAddOverwritesZeroIndex() {
                    list.add(capacity - 1, insertion);

                    assertEquals(value, list.get(0));
                    assertEquals(insertion, list.get(capacity - 1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertAllMatchValue(list.subList(1, list.size()));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    try {
                        list.get(1);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //
                    }
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    try {
                        list.get(1);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //
                    }
                    assertEquals(size - 1, list.size());
                }
            }
        }

        public class AndZeroIndexIsMiddle {

            @Before
            public void setup() {
                FOR(capacity + halfCapacity, () -> list.add(value));
                FOR(capacity, () -> list.remove(0));
            }

            public class AndListIsEmpty {

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void changeRemoveDoesNotWork() {
                    assertFalse(list.remove("some string"));
                }

                @Test
                public void indexRemoveDoesNotWork() {
                    try {
                        list.remove(0);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // cannot remove when list is empty
                    }
                }

            }

            public class AndListIsHalfFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(halfInsertionIndex, insertion);

                    assertEquals(insertion, list.get(0));
                    assertEquals(firstValue, list.get(1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(firstValue);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(0);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

            }

            public class AndListIsFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    FOR(capacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddOverwritesZeroIndex() {
                    list.add(insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 1));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddOverwritesZeroIndex() {
                    list.add(capacity - 1, insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(0);

                    assertEquals(value, list.get(0));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(size - 1, list.size());
                }

            }

        }

        public class AndZeroIndexIsLastIndex {

            @Before
            public void setup() {
                FOR(capacity + capacity - 1, () -> list.add(value));
                FOR(list.size(), () -> list.remove(0));
            }

            public class AndListIsEmpty {

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(0, insertion);

                    assertEquals(insertion, list.get(0));
                }

                @Test
                public void changeRemoveDoesNotWork() {
                    assertFalse(list.remove("some string"));
                }

                @Test
                public void indexRemoveDoesNotWork() {
                    try {
                        list.remove(0);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // cannot remove when list is empty
                    }
                }

            }

            public class AndListIsHalfFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                }

                @Test
                public void changeAddWorks() {
                    list.add(insertion);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddWorks() {
                    list.add(halfInsertionIndex, insertion);

                    assertEquals(insertion, list.get(0));
                    assertEquals(firstValue, list.get(1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(firstValue);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(0);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveDoesNotWork() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertTrue(list.isEmpty());
                    assertEquals(size - 1, list.size());
                }

            }

            public class AndListIsFull {

                @Before
                public void setup() {
                    list.add(firstValue);
                    FOR(capacity - 1, () -> list.add(value));
                }

                @Test
                public void changeAddOverwritesZeroIndex() {
                    list.add(insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void indexAndChangeAddOverwritesZeroIndex() {
                    list.add(list.size() - 1, insertion);

                    assertAllMatchValue(list.subList(0, list.size() - 2));
                    assertEquals(insertion, list.get(list.size() - 1));
                }

                @Test
                public void changeRemoveWorks() {
                    int size = list.size();
                    list.remove(value);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void indexRemoveWorks() {
                    int size = list.size();
                    list.remove(1);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(size - 1, list.size());
                }

                @Test
                public void lastIndexRemoveWorks() {
                    int size = list.size();
                    list.remove(list.size() - 1);

                    assertEquals(firstValue, list.get(0));
                    assertEquals(size - 1, list.size());
                }
            }
        }

    }

    private void assertAllMatchValue(List<String> l) {
        assertTrue(l.stream().allMatch(s -> s.equals(value)));
    }
}

