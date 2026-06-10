package org.insa.graphs.algorithm.utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Binary min-heap with an O(1) reverse index backed by an integer array.
 * <p>
 * Elements must implement {@link Identifiable}: their {@link Identifiable#getId()}
 * value is used as the key into the {@code position} array, which stores the current
 * index of each element in the heap. This turns {@link #remove} from O(n) (linear scan)
 * into O(log n), which is the critical improvement for Dijkstra's decrease-key step.
 * </p>
 * <p>
 * The heap is constructed with a {@code maxNodeId} capacity: IDs must satisfy
 * {@code 0 <= id < maxNodeId}.
 * </p>
 *
 * @param <E> Element type; must be {@link Comparable} and {@link Identifiable}.
 */
public class IndexedBinaryHeap<E extends Comparable<E> & Identifiable>
        implements PriorityQueue<E> {

    private int currentSize;
    protected final ArrayList<E> array;

    // position[id] = index of element with getId()==id in array[], or -1 if absent.
    private final int[] position;

    /**
     * Construct an empty indexed heap.
     *
     * @param maxNodeId Upper bound (exclusive) on element IDs stored in this heap.
     */
    public IndexedBinaryHeap(int maxNodeId) {
        this.currentSize = 0;
        this.array = new ArrayList<>();
        this.position = new int[maxNodeId];
        Arrays.fill(this.position, -1);
    }

    // --- index helpers ---

    private int indexParent(int index) {
        return (index - 1) / 2;
    }

    private int indexLeft(int index) {
        return index * 2 + 1;
    }

    // --- core: every element move goes through arraySet ---

    private void arraySet(int index, E value) {
        if (index == this.array.size()) {
            this.array.add(value);
        }
        else {
            this.array.set(index, value);
        }
        position[value.getId()] = index;
    }

    private void percolateUp(int index) {
        E x = this.array.get(index);
        for (; index > 0 && x.compareTo(this.array.get(indexParent(index))) < 0; index =
                indexParent(index)) {
            arraySet(index, this.array.get(indexParent(index)));
        }
        arraySet(index, x);
    }

    private void percolateDown(int index) {
        int ileft = indexLeft(index);
        int iright = ileft + 1;

        if (ileft < this.currentSize) {
            E current = this.array.get(index);
            E left = this.array.get(ileft);
            boolean hasRight = iright < this.currentSize;
            E right = hasRight ? this.array.get(iright) : null;

            if (!hasRight || left.compareTo(right) < 0) {
                if (left.compareTo(current) < 0) {
                    arraySet(index, left);
                    arraySet(ileft, current);
                    percolateDown(ileft);
                }
            }
            else {
                if (right.compareTo(current) < 0) {
                    arraySet(index, right);
                    arraySet(iright, current);
                    percolateDown(iright);
                }
            }
        }
    }

    // --- PriorityQueue interface ---

    @Override
    public boolean isEmpty() {
        return this.currentSize == 0;
    }

    @Override
    public int size() {
        return this.currentSize;
    }

    @Override
    public void insert(E x) {
        int index = this.currentSize++;
        arraySet(index, x);
        percolateUp(index);
    }

    @Override
    public void remove(E x) throws ElementNotFoundException {
        int index = position[x.getId()];
        if (index < 0 || index >= this.currentSize
                || !this.array.get(index).equals(x)) {
            throw new ElementNotFoundException(x);
        }

        position[x.getId()] = -1;
        --this.currentSize;

        if (index < this.currentSize) {
            arraySet(index, this.array.get(this.currentSize));
            percolateDown(index);
            percolateUp(index);
        }
    }

    @Override
    public E findMin() throws EmptyPriorityQueueException {
        if (isEmpty())
            throw new EmptyPriorityQueueException();
        return this.array.get(0);
    }

    @Override
    public E deleteMin() throws EmptyPriorityQueueException {
        E minItem = findMin();
        position[minItem.getId()] = -1;
        --this.currentSize;
        if (this.currentSize > 0) {
            arraySet(0, this.array.get(this.currentSize));
            percolateDown(0);
        }
        return minItem;
    }

}
