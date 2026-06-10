package org.insa.graphs.algorithm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class IndexedBinaryHeapTest {

    /**
     * Integer element with a stable unique ID, suitable for IndexedBinaryHeap.
     * Comparison is by value; ties broken by id (to match MutableInteger semantics).
     */
    private static class IdInteger implements Comparable<IdInteger>, Identifiable {
        private int value;
        private final int id;
        private static int counter = 0;

        IdInteger(int value) {
            this.value = value;
            this.id = counter++;
        }

        int get() {
            return value;
        }

        void set(int v) {
            value = v;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int compareTo(IdInteger other) {
            if (this.id == other.id)
                return 0;
            return Integer.compare(this.value, other.value);
        }

        @Override
        public String toString() {
            return value + "[id:" + id + "]";
        }
    }

    // Capacity: IDs are assigned sequentially starting from 0 at class load.
    // We allocate plenty of headroom so all test instances fit.
    private static final int CAPACITY = 10_000;

    private IndexedBinaryHeap<IdInteger> heap;

    @Before
    public void setUp() {
        heap = new IndexedBinaryHeap<>(CAPACITY);
    }

    // --- basic structural tests ---

    @Test
    public void testEmptyOnCreation() {
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
    }

    @Test(expected = EmptyPriorityQueueException.class)
    public void testFindMinOnEmpty() throws EmptyPriorityQueueException {
        heap.findMin();
    }

    @Test(expected = EmptyPriorityQueueException.class)
    public void testDeleteMinOnEmpty() throws EmptyPriorityQueueException {
        heap.deleteMin();
    }

    @Test(expected = ElementNotFoundException.class)
    public void testRemoveFromEmpty() throws ElementNotFoundException {
        heap.remove(new IdInteger(0));
    }

    // --- insert / findMin / deleteMin ---

    @Test
    public void testInsertAndFindMin() {
        IdInteger a = new IdInteger(5);
        IdInteger b = new IdInteger(2);
        IdInteger c = new IdInteger(8);
        heap.insert(a);
        heap.insert(b);
        heap.insert(c);
        assertEquals(3, heap.size());
        assertEquals(2, heap.findMin().get());
    }

    @Test
    public void testDeleteMinReturnsInOrder() {
        int[] values = { 8, 1, 6, 3, 4, 5, 9 };
        IdInteger[] elems = new IdInteger[values.length];
        for (int i = 0; i < values.length; i++) {
            elems[i] = new IdInteger(values[i]);
            heap.insert(elems[i]);
        }

        int[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);

        for (int v : sorted) {
            assertEquals(v, heap.deleteMin().get());
        }
        assertTrue(heap.isEmpty());
    }

    // --- remove ---

    @Test(expected = ElementNotFoundException.class)
    public void testRemoveNotPresent() throws ElementNotFoundException {
        heap.insert(new IdInteger(3));
        heap.remove(new IdInteger(99)); // different object, not in heap
    }

    @Test
    public void testRemoveMiddleElement() {
        IdInteger a = new IdInteger(5);
        IdInteger b = new IdInteger(2);
        IdInteger c = new IdInteger(8);
        heap.insert(a);
        heap.insert(b);
        heap.insert(c);

        heap.remove(a); // remove the middle value
        assertEquals(2, heap.size());
        assertEquals(2, heap.deleteMin().get());
        assertEquals(8, heap.deleteMin().get());
        assertTrue(heap.isEmpty());
    }

    @Test
    public void testRemoveMin() {
        IdInteger a = new IdInteger(1);
        IdInteger b = new IdInteger(5);
        heap.insert(a);
        heap.insert(b);

        heap.remove(a);
        assertEquals(1, heap.size());
        assertEquals(5, heap.findMin().get());
    }

    @Test
    public void testRemoveLast() {
        IdInteger a = new IdInteger(1);
        IdInteger b = new IdInteger(5);
        heap.insert(a);
        heap.insert(b);

        heap.remove(b);
        assertEquals(1, heap.size());
        assertEquals(1, heap.findMin().get());
    }

    @Test
    public void testSingleElementDeleteMin() {
        IdInteger a = new IdInteger(42);
        heap.insert(a);
        assertEquals(42, heap.deleteMin().get());
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
        // Subsequent insert must still work (position array not corrupted).
        IdInteger b = new IdInteger(7);
        heap.insert(b);
        assertEquals(7, heap.deleteMin().get());
    }

    @Test(expected = ElementNotFoundException.class)
    public void testRemoveAfterDeleteMin() throws ElementNotFoundException {
        IdInteger a = new IdInteger(3);
        heap.insert(a);
        heap.deleteMin();
        heap.remove(a); // must throw: a is no longer in the heap
    }

    @Test(expected = ElementNotFoundException.class)
    public void testRemoveTwice() throws ElementNotFoundException {
        IdInteger a = new IdInteger(3);
        IdInteger b = new IdInteger(7);
        heap.insert(a);
        heap.insert(b);
        heap.remove(a);
        heap.remove(a); // must throw
    }

    // --- decrease-key (Dijkstra pattern) ---

    @Test
    public void testDecreaseKey() {
        IdInteger[] elems = { new IdInteger(10), new IdInteger(20), new IdInteger(30),
                new IdInteger(40), new IdInteger(50) };
        for (IdInteger e : elems)
            heap.insert(e);

        // Decrease elems[3] from 40 to 5 (Dijkstra relax pattern).
        heap.remove(elems[3]);
        elems[3].set(5);
        heap.insert(elems[3]);

        // Expected order: 5, 10, 20, 30, 50
        int[] expected = { 5, 10, 20, 30, 50 };
        for (int v : expected) {
            assertEquals(v, heap.deleteMin().get());
        }
        assertTrue(heap.isEmpty());
    }

    // --- heap-order invariant after many mixed operations ---

    @Test
    public void testMixedOperationsPreserveOrder() {
        List<IdInteger> inserted = new ArrayList<>();
        for (int v : new int[] { 7, 3, 11, 1, 9, 5, 13, 2, 6 }) {
            IdInteger e = new IdInteger(v);
            inserted.add(e);
            heap.insert(e);
        }

        // Remove a few elements by value reference.
        heap.remove(inserted.get(2)); // 11
        heap.remove(inserted.get(0)); // 7

        List<Integer> remaining = new ArrayList<>();
        for (IdInteger e : inserted)
            remaining.add(e.get());
        remaining.remove(Integer.valueOf(11));
        remaining.remove(Integer.valueOf(7));
        Collections.sort(remaining);

        List<Integer> actual = new ArrayList<>();
        while (!heap.isEmpty())
            actual.add(heap.deleteMin().get());

        assertEquals(remaining, actual);
    }

}
