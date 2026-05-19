package org.insa.graphs.algorithm.shortestpath;

import java.util.Arrays;

import org.insa.graphs.model.Graph;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Point;
import org.insa.graphs.model.RoadInformation;
import org.insa.graphs.model.RoadInformation.RoadType;

/**
 * Hand-built graphs with fully known answers, used as the controlled (non-road) test
 * cases. Same construction pattern as {@code PathTest}: {@link Node#linkNodes} on plain
 * {@link Node}s.
 * <p>
 * Every node shares a single {@link Point}, so the A* heuristic
 * {@code Point.distance(...)} evaluates to {@code 0} on these graphs: a valid (trivial)
 * lower bound, hence A* stays optimal here once it is repaired. All arcs are one-way,
 * so the graph topology is exactly what is written.
 */
final class SmallGraphs {

    private SmallGraphs() {
        // Utility class.
    }

    /** Shared position for every hand-built node (heuristic distance = 0). */
    private static final Point POINT = new Point(1.4f, 43.6f);

    /** One-way residential road, 50 km/h, used for every hand-built arc. */
    private static final RoadInformation ROAD =
            new RoadInformation(RoadType.RESIDENTIAL, null, true, 50, "");

    private static Node[] newNodes(int count) {
        Node[] nodes = new Node[count];
        for (int i = 0; i < count; i++) {
            nodes[i] = new Node(i, POINT);
        }
        return nodes;
    }

    /**
     * Diamond graph (5 nodes, one-way arcs):
     *
     * <pre>
     *        4 (len)        1
     *    (0) ------> (1) ------> (3) ---1---> (4)
     *     |  1                    ^
     *     +--------> (2) ---5------+
     * </pre>
     *
     * Shortest path 0 to 4 is 0-1-3-4 with cost 4+1+1 = <b>6</b>. Note that a greedy
     * first step would take the cheap 0 to 2 arc (cost 1) and miss the optimum: a real
     * test for Dijkstra. Node 4 is a sink, so 4 to 0 has no path (infeasible scenario).
     *
     * @return The diamond graph (map id {@code "small-diamond"}).
     */
    static Graph diamond() {
        Node[] n = newNodes(5);
        Node.linkNodes(n[0], n[1], 4f, ROAD, null);
        Node.linkNodes(n[0], n[2], 1f, ROAD, null);
        Node.linkNodes(n[1], n[3], 1f, ROAD, null);
        Node.linkNodes(n[2], n[3], 5f, ROAD, null);
        Node.linkNodes(n[3], n[4], 1f, ROAD, null);
        return new Graph("small-diamond", "small-diamond", Arrays.asList(n), null);
    }

    /**
     * Graph with two distinct optimal paths of equal cost (4 nodes, one-way):
     *
     * <pre>
     *        3          2
     *    (0) --> (1) --> (3)
     *     |               ^
     *     +--> (2) --------+
     *       3          2
     * </pre>
     *
     * Both 0-1-3 and 0-2-3 cost <b>5</b>. Dijkstra and Bellman-Ford may return
     * different paths: a scenario to check that we compare <em>costs</em>, not the arcs
     * themselves.
     *
     * @return The two-equal-paths graph (map id {@code "small-two-equal"}).
     */
    static Graph twoEqualPaths() {
        Node[] n = newNodes(4);
        Node.linkNodes(n[0], n[1], 3f, ROAD, null);
        Node.linkNodes(n[0], n[2], 3f, ROAD, null);
        Node.linkNodes(n[1], n[3], 2f, ROAD, null);
        Node.linkNodes(n[2], n[3], 2f, ROAD, null);
        return new Graph("small-two-equal", "small-two-equal", Arrays.asList(n), null);
    }
}
