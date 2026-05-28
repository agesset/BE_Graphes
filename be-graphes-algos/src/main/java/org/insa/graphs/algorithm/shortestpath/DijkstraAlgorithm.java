package org.insa.graphs.algorithm.shortestpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.algorithm.utils.BinaryHeap;
import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Graph;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Path;

/**
 * <p>
 * Implementation of Dijkstra's algorithm for the single-source shortest-path problem.
 * </p>
 * <p>
 * Nodes are explored in increasing order of their cost from the origin, using a
 * {@link BinaryHeap} of {@link Label}s as the priority queue. The algorithm assumes
 * that every arc cost is non-negative.
 * </p>
 * <p>
 * Label creation is delegated to the {@link #newLabel} factory method, which
 * {@link AStarAlgorithm} overrides to turn this very routine into an A* search: the
 * main loop below is therefore shared by both algorithms.
 * </p>
 */
public class DijkstraAlgorithm extends ShortestPathAlgorithm {

    /** {@code true} when this instance is running as a comeback search inside {@link MarathonAlgorithm}. */
    protected boolean isMarathoned;

    /** Node IDs that must be skipped during the search; non-{@code null} only in marathon mode. */
    protected Set<Integer> forbiddenNodes;

    /**
     * Create a Dijkstra algorithm instance for the given shortest-path problem.
     *
     * @param data Input data describing the graph, origin, destination and cost.
     */
    public DijkstraAlgorithm(ShortestPathData data) {
        super(data);
        this.isMarathoned = false;
    }

    /**
     * Create a Dijkstra instance configured as a comeback search for {@link MarathonAlgorithm}.
     * Nodes whose IDs appear in {@code forbiddenNodes} are silently skipped during the
     * search so that the return leg cannot revisit nodes already on the outward leg.
     *
     * @param data Input data describing the graph, start node, destination and cost.
     * @param forbiddenNodes Set of node IDs that the search must not visit.
     */
    public DijkstraAlgorithm(ShortestPathData data, Set<Integer> forbiddenNodes) {
        super(data);
        this.isMarathoned = true;
        this.forbiddenNodes = forbiddenNodes;
    }

    /**
     * Run Dijkstra's algorithm and build the corresponding solution.
     *
     * @return A {@link ShortestPathSolution}: {@code OPTIMAL} together with the
     *         shortest path when the destination is reachable, {@code INFEASIBLE}
     *         otherwise.
     */
    @Override
    protected ShortestPathSolution doRun() {

        // Input problem (getInputData() is inherited from ShortestPathAlgorithm).
        final ShortestPathData data = getInputData();

        // Graph on which the shortest path is searched.
        Graph graph = data.getGraph();

        final int nbNodes = graph.size();

        // Trivial case: origin and destination are the same node, the shortest path is
        // the empty path reduced to that single node.
        if (data.getOrigin().equals(data.getDestination())) {
            notifyOriginProcessed(data.getOrigin());
            notifyNodeMarked(data.getOrigin());
            notifyDestinationReached(data.getDestination());
            return new ShortestPathSolution(data, Status.OPTIMAL,
                    new Path(graph, data.getOrigin()));
        }

        // One label per node, indexed by node id; null until the node is reached.
        Label[] labels = new Label[nbNodes];
        Arrays.fill(labels, null);
        labels[data.getOrigin().getId()] =
                this.newLabel(data.getOrigin(), false, 0.0, null);

        // Priority queue holding the reached but not yet marked labels.
        BinaryHeap<Label> heap = new BinaryHeap<Label>();
        heap.insert(labels[data.getOrigin().getId()]);

        // Notify observers about the first event (origin processed).
        notifyOriginProcessed(data.getOrigin());

        // Main loop. Arc costs are assumed to be non-negative, so the first time a node
        // is extracted from the heap its cost is already optimal.
        boolean found = false;

        while (!found && !heap.isEmpty()) {

            // Extract the cheapest reached label. Skip forbidden nodes; mark all others
            // as final (their cost is now optimal).
            Label currentLabel = heap.deleteMin();

            if (this.isCancelled(currentLabel.getCurrentNode().getId())) {
                continue;
            }

            currentLabel.mark();
            notifyNodeMarked(currentLabel.getCurrentNode());

            // Stop as soon as the destination has been marked.
            if (currentLabel.getCurrentNode().equals(data.getDestination())) {
                found = true;
                break;
            }

            // Relax every outgoing arc of the current node.
            for (Arc successor : currentLabel.getCurrentNode().getSuccessors()) {

                // Skip arcs forbidden by the current filter (e.g. one-way roads).
                if (!data.isAllowed(successor)) {
                    continue;
                }

                // Cost of reaching the successor through the current node.
                double newCost = currentLabel.getCost() + data.getCost(successor);
                Label successorLabel = labels[successor.getDestination().getId()];

                if (successorLabel == null) {
                    // Successor reached for the first time: create its label.
                    successorLabel = this.newLabel(successor.getDestination(), false,
                            newCost, successor);

                    labels[successor.getDestination().getId()] = successorLabel;
                    heap.insert(successorLabel);

                    notifyNodeReached(successor.getDestination());
                }
                else if (!successorLabel.isMarked()
                        && successorLabel.getCost() > newCost) {
                    // A cheaper path to an already reached node was found: decrease its
                    // key (remove, update, then re-insert it into the heap).
                    heap.remove(successorLabel);
                    successorLabel.setCost(newCost);
                    successorLabel.setPredecessorArc(successor);
                    heap.insert(successorLabel);
                }
            }
        }

        // Solution of the shortest-path problem, built below.
        ShortestPathSolution solution = null;

        // The destination was never marked: it is unreachable from the origin.
        if (labels[data.getDestination().getId()] == null
                || !labels[data.getDestination().getId()].isMarked()) {
            solution = new ShortestPathSolution(data, Status.INFEASIBLE);
        }
        else {
            notifyDestinationReached(data.getDestination());

            // Rebuild the path by following the predecessor arcs backwards, from the
            // destination up to the origin.
            ArrayList<Arc> arcs = new ArrayList<>();
            Arc arc = labels[data.getDestination().getId()].getPredecessorArc();
            while (arc != null) {
                arcs.add(arc);
                arc = labels[arc.getOrigin().getId()].getPredecessorArc();
            }

            // Arcs were collected destination-to-origin: restore the correct order.
            Collections.reverse(arcs);

            // Wrap the arcs into the final optimal solution.
            solution = new ShortestPathSolution(data, Status.OPTIMAL,
                    new Path(graph, arcs));
        }

        return solution;
    }

    /**
     * Factory method building the label of a node as it is discovered. Dijkstra creates
     * a plain label with no heuristic; {@link AStarAlgorithm} overrides this method to
     * attach an estimated remaining cost and thus obtain an A* search.
     *
     * @param currentNode Node the label refers to.
     * @param marked Whether the node is already marked.
     * @param cost Cost of the best known path from the origin to the node.
     * @param predecessorArc Arc used to reach the node, or {@code null} for the origin.
     * @return A new label for {@code currentNode}.
     */
    public Label newLabel(Node currentNode, boolean marked, double cost,
            Arc predecessorArc) {
        return new Label(currentNode, marked, cost, predecessorArc);
    }

    /**
     * Return {@code true} if the node with the given ID must be skipped during the search.
     * In marathon mode, nodes that already belong to the outward path are forbidden so that
     * the comeback leg forms a proper circuit without reusing outward-leg nodes.
     *
     * @param nodeId Identifier of the node to test.
     * @return {@code true} if and only if this instance is in marathon mode and
     *         {@code nodeId} is in the forbidden set.
     */
    public boolean isCancelled(int nodeId) {
        return this.isMarathoned && forbiddenNodes.contains(nodeId);
    }
}
