package org.insa.graphs.algorithm.utils;

/**
 * Implemented by any element that can be stored in an {@link IndexedBinaryHeap}. The
 * integer returned by {@link #getId()} must be stable (never change for a given object)
 * and unique within the heap's declared capacity.
 */
public interface Identifiable {
    int getId();
}
