package org.insa.graphs.algorithm.shortestpath;

/**
 * Runs the full {@link AbstractShortestPathTest} suite against
 * {@link DijkstraAlgorithm}. All the logic lives in the parent class; this subclass
 * only names the algorithm to instantiate.
 */
public class DijkstraAlgorithmTest extends AbstractShortestPathTest {

    @Override
    protected ShortestPathAlgorithm newAlgorithm(ShortestPathData data) {
        return new DijkstraAlgorithm(data);
    }
}
