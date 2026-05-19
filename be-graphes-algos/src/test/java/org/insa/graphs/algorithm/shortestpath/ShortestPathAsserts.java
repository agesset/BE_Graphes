package org.insa.graphs.algorithm.shortestpath;

import static org.junit.Assert.assertEquals;

import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Path;

/**
 * Shared helpers for the shortest-path test suite.
 * <p>
 * Gathered here so that {@link AbstractShortestPathTest} and
 * {@link DijkstraVsAStarTest} reuse the exact same cost computation and tolerance,
 * instead of copy-pasting it.
 */
final class ShortestPathAsserts {

    private ShortestPathAsserts() {
        // Utility class.
    }

    /**
     * Cost of a path as the algorithm would sum it: the cost given by the arc inspector
     * for every arc, accumulated as a {@code double}.
     *
     * @param data Input data carrying the arc inspector (cost function).
     * @param path Path to evaluate.
     * @return Total cost of the path according to {@code data}.
     */
    static double costAlongPath(ShortestPathData data, Path path) {
        double cost = 0.0;
        for (Arc arc : path.getArcs()) {
            cost += data.getCost(arc);
        }
        return cost;
    }

    /**
     * Tolerance used to compare two real costs. Real numbers are never compared with
     * {@code ==}: costs are accumulated differently (an algorithm sums {@code double}s,
     * {@link Path#getLength()} sums {@code float}s), so a small relative error is
     * expected. We allow 0.1% of the expected value, with a 1e-3 absolute floor for
     * values close to zero.
     *
     * @param expected Reference value.
     * @return Acceptable absolute error around {@code expected}.
     */
    static double tolerance(double expected) {
        return Math.max(1e-3, 1e-3 * Math.abs(expected));
    }

    /**
     * Assert that two costs are equal up to {@link #tolerance(double)}.
     *
     * @param message Message reported on failure.
     * @param expected Expected cost.
     * @param actual Actual cost.
     */
    static void assertCostEquals(String message, double expected, double actual) {
        assertEquals(message, expected, actual, tolerance(expected));
    }
}
