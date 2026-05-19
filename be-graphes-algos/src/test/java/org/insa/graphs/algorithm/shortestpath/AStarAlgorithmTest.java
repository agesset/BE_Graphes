package org.insa.graphs.algorithm.shortestpath;

/**
 * Runs the full {@link AbstractShortestPathTest} suite against {@link AStarAlgorithm}
 * &mdash; the modular suite reused as-is, only the algorithm changes.
 * <p>
 * A* must satisfy exactly the same checks as Dijkstra (valid path, cost consistency,
 * agreement with Bellman-Ford, sub-path optimality, known optimal costs). The heuristic
 * A* uses (geometric distance, divided by the maximum speed in time mode) is
 * consistent, so A* returns optimal costs and the whole suite passes.
 */
public class AStarAlgorithmTest extends AbstractShortestPathTest {

    @Override
    protected ShortestPathAlgorithm newAlgorithm(ShortestPathData data) {
        return new AStarAlgorithm(data);
    }
}
