package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Arc;
import org.insa.graphs.model.GraphStatistics;
import org.insa.graphs.model.Node;

/**
 * <p>
 * Implementation of the A* algorithm for the single-source shortest-path problem.
 * </p>
 * <p>
 * A* is {@link DijkstraAlgorithm} guided by a heuristic. It reuses the entire Dijkstra
 * routine and only overrides the {@link #newLabel} factory, so that labels are ordered
 * by their total cost <em>f = g + h</em>, where <em>g</em> is the cost from the origin
 * and <em>h</em> estimates the remaining cost to the destination.
 * </p>
 * <p>
 * The heuristic is based on the straight-line (geometric) distance from a node to the
 * destination:
 * </p>
 * <ul>
 * <li>in {@code LENGTH} and {@code PEDESTRIAN_LENGTH} modes that distance is used
 * directly;</li>
 * <li>in {@code TIME} mode it is divided by the maximum speed, which turns it into the
 * shortest conceivable travel time.</li>
 * </ul>
 * <p>
 * That estimate never exceeds the real remaining cost, so the heuristic is admissible
 * and A* still returns optimal paths.
 * </p>
 */
public class AStarAlgorithm extends DijkstraAlgorithm {

    /**
     * Create an A* algorithm instance for the given shortest-path problem.
     *
     * @param data Input data describing the graph, origin, destination and cost.
     */
    public AStarAlgorithm(ShortestPathData data) {
        super(data);
    }

    /**
     * Create an A* instance configured as a comeback search for {@link MarathonAlgorithm}.
     * The forbidden-nodes array is forwarded to the parent {@link DijkstraAlgorithm} so
     * that the return leg avoids nodes already on the outward path.
     *
     * @param data Input data describing the graph, start node, destination and cost.
     * @param forbiddenNodes Boolean array indexed by node ID; a {@code true} entry marks
     *        that node as forbidden during the search.
     */
    public AStarAlgorithm(ShortestPathData data, boolean[] forbiddenNodes, double lambda) {
        super(data, forbiddenNodes, lambda);
    }

    /**
     * Build an A* label: the same data as a Dijkstra label, augmented with the
     * heuristic estimate of the remaining cost from {@code currentNode} to the
     * destination. This override is what turns the inherited Dijkstra routine into an
     * A* search.
     *
     * @param currentNode Node the label refers to.
     * @param marked Whether the node is already marked.
     * @param cost Cost of the best known path from the origin to the node.
     * @param predecessorArc Arc used to reach the node, or {@code null} for the origin.
     * @return A new label carrying the estimated remaining cost. In {@code LENGTH} and
     *         {@code PEDESTRIAN_LENGTH} modes the estimate is the straight-line
     *         distance; in {@code TIME} mode it is divided by the maximum speed
     *         (falling back to the raw distance when no maximum speed is available in
     *         the graph statistics).
     * @throws IllegalArgumentException if the cost mode is not LENGTH,
     *         PEDESTRIAN_LENGTH, or TIME.
     */
    public Label newLabel(Node currentNode, boolean marked, double cost,
            Arc predecessorArc) {
        if (getInputData().getMode() == ShortestPathData.Mode.LENGTH || getInputData()
                .getMode() == ShortestPathData.Mode.PEDESTRIAN_LENGTH) {
            // LENGTH / PEDESTRIAN_LENGTH: the estimate is the geometric distance still
            // to be covered to reach the destination.
            return new Label(
                    currentNode, marked, cost, getInputData().getDestination()
                            .getPoint().distanceTo(currentNode.getPoint()),
                    predecessorArc);
        }
        else if (getInputData().getMode() == ShortestPathData.Mode.TIME) {

            if (getInputData().getMaximumSpeed() == GraphStatistics.NO_MAXIMUM_SPEED) {
                // Time mode but no maximum speed is known: fall back to the raw
                // geometric distance as the estimate.
                return new Label(
                        currentNode, marked, cost, (getInputData().getDestination()
                                .getPoint().distanceTo(currentNode.getPoint())),
                        predecessorArc);
            }
            // Time mode: the estimate is the geometric distance divided by the maximum
            // speed, i.e. the shortest possible travel time to the destination.
            return new Label(currentNode, marked, cost,
                    (getInputData().getDestination().getPoint().distanceTo(
                            currentNode.getPoint()) / getInputData().getMaximumSpeed()),
                    predecessorArc);
        }
        // Defensive guard: ShortestPathData only supports LENGTH, PEDESTRIAN_LENGTH and
        // TIME.
        throw new IllegalArgumentException(
                "Unsupported mode: " + getInputData().getMode());
    }

}
