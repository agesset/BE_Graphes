package org.insa.graphs.algorithm.shortestpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.algorithm.utils.BinaryHeap;
import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Graph;
import org.insa.graphs.model.Path;

/**
 * Algorithm that searches for a closed pedestrian circuit whose total length equals the
 * marathon distance (42,195 m).
 *
 * <p>
 * The search proceeds in two phases:
 * </p>
 * <ol>
 * <li><em>Outward exploration.</em>  A priority-queue search driven by {@link LabelMarathon}
 *     expands nodes outward from the origin.  Labels are ordered by how close the optimistic
 *     circuit estimate — outward cost plus straight-line return distance — is to the marathon
 *     target, so the most promising nodes are expanded first.</li>
 * <li><em>Comeback search.</em>  When a node's optimistic estimate falls within a tolerance
 *     window ({@code lambda = 4,220 m}) around the target, a {@link DijkstraAlgorithm}
 *     comeback is launched from that node to the destination.  The comeback is forbidden from
 *     revisiting any node already on the outward path, ensuring a proper circuit without
 *     repeated nodes.</li>
 * </ol>
 * <p>
 * A solution is accepted when the combined outward and comeback length exactly equals the
 * marathon distance ({@code errorMargin = 0 m}).
 * </p>
 * <p>
 * Only {@link ShortestPathData.Mode#PEDESTRIAN_LENGTH} is supported; any other mode
 * immediately returns {@link org.insa.graphs.algorithm.AbstractSolution.Status#INFEASIBLE}.
 * </p>
 */
public class MarathonAlgorithm extends ShortestPathAlgorithm {

    /**
     * Create a marathon-algorithm instance for the given shortest-path problem.
     *
     * @param data Input data describing the graph, origin, destination and cost inspector.
     *             The {@link ShortestPathData#getMode() mode} must be
     *             {@link ShortestPathData.Mode#PEDESTRIAN_LENGTH}.
     */
    public MarathonAlgorithm(ShortestPathData data) {
        super(data);
    }

    /**
     * Execute the marathon-circuit search and return the solution.
     *
     * @return A {@link ShortestPathSolution}: {@code OPTIMAL} with the circuit path when a
     *         valid marathon-length loop is found; {@code INFEASIBLE} if the input mode is
     *         not {@code PEDESTRIAN_LENGTH} or if no circuit of the required length exists.
     */
    @Override
    protected ShortestPathSolution doRun() {

        // Input problem (getInputData() is inherited from ShortestPathAlgorithm).
        ShortestPathData data = getInputData();

        if (data.getMode() != ShortestPathData.Mode.PEDESTRIAN_LENGTH) {
            return new ShortestPathSolution(data, Status.INFEASIBLE);
        }

        double marathonLength = 42195;
        double lambda = 4220;
        double errorMargin = 50;

        LabelMarathon comebackLabel = null;
        ShortestPathSolution comebackSolution = null;

        // Graph on which the shortest path is searched.
        Graph graph = data.getGraph();

        final int nbNodes = graph.size();

        // One label per node, indexed by node id; null until the node is reached.
        LabelMarathon[] labels = new LabelMarathon[nbNodes];
        Arrays.fill(labels, null);
        labels[data.getOrigin().getId()] =
                new LabelMarathon(data.getOrigin(), false, 0.0,
                        data.getOrigin().getPoint()
                                .distanceTo(data.getDestination().getPoint()),
                        null, marathonLength);

        // Priority queue holding the reached but not yet marked labels.
        BinaryHeap<LabelMarathon> heap = new BinaryHeap<LabelMarathon>();
        heap.insert(labels[data.getOrigin().getId()]);

        // Notify observers about the first event (origin processed).
        notifyOriginProcessed(data.getOrigin());

        // Main loop. Arc costs are assumed to be non-negative, so the first time a node
        // is extracted from the heap its cost is already optimal.
        boolean found = false;

        while (!found && !heap.isEmpty()) {

            // Extract the cheapest reached label and mark it: its cost is now final.
            LabelMarathon currentLabel = heap.deleteMin();
            currentLabel.mark();

            notifyNodeMarked(currentLabel.getCurrentNode());

            // If the estimated total circuit length (cost so far + straight-line back
            // to
            // destination) is within the lambda window around the marathon target, try
            // a
            // Dijkstra comeback from this node. This fires at most once per extracted
            // node
            // and only when the estimate is plausible — avoiding thousands of Dijkstra
            // calls.
            double estimatedTotal =
                    currentLabel.getCost() + currentLabel.getEstimatedCost();
            if (estimatedTotal >= marathonLength - lambda
                    && estimatedTotal <= marathonLength + lambda) {

                Set<Integer> outwardPathNodes = new HashSet<>();
                Arc pathArc = labels[currentLabel.getCurrentNode().getId()]
                        .getPredecessorArc();
                while (pathArc != null) {
                    outwardPathNodes.add(pathArc.getDestination().getId());
                    outwardPathNodes.add(pathArc.getOrigin().getId());
                    pathArc = labels[pathArc.getOrigin().getId()].getPredecessorArc();
                }
                outwardPathNodes.remove(currentLabel.getCurrentNode().getId());
                // For a closed circuit origin == destination; ensure the comeback can
                // reach its target even though the origin was recorded as a path node.
                outwardPathNodes.remove(data.getDestination().getId());

                comebackSolution = new DijkstraAlgorithm(
                        new ShortestPathData(graph, currentLabel.getCurrentNode(),
                                data.getDestination(), data.getArcInspector()),
                        outwardPathNodes).doRun();

                if (comebackSolution.getStatus() == Status.OPTIMAL) {
                    double totalLength = currentLabel.getCost()
                            + comebackSolution.getPath().getLength();
                    if (totalLength >= marathonLength - errorMargin
                            && totalLength <= marathonLength + errorMargin) {
                        comebackLabel = currentLabel;
                        found = true;
                    }
                }

                if (!found) {
                    comebackSolution = null;
                }
            }

            if (found)
                break;

            // Relax every outgoing arc of the current node.
            for (Arc successor : currentLabel.getCurrentNode().getSuccessors()) {

                // Skip arcs forbidden by the current filter (e.g. one-way roads).
                if (!data.isAllowed(successor)
                        || successor.getDestination().equals(data.getDestination())) {
                    continue;
                }

                // Cost of reaching the successor through the current node.
                double newCost = currentLabel.getCost() + data.getCost(successor);
                LabelMarathon successorLabel =
                        labels[successor.getDestination().getId()];

                if (successorLabel == null) {
                    // Successor reached for the first time: create its label.
                    successorLabel = new LabelMarathon(successor.getDestination(),
                            false, newCost,
                            successor.getDestination().getPoint()
                                    .distanceTo(data.getDestination().getPoint()),
                            successor, marathonLength);

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

        if (!found) {
            return new ShortestPathSolution(data, Status.INFEASIBLE);
        }

        ArrayList<Arc> arcs = new ArrayList<>();
        Arc arc = labels[comebackLabel.getCurrentNode().getId()].getPredecessorArc();

        while (arc != null) {
            arcs.add(arc);
            arc = labels[arc.getOrigin().getId()].getPredecessorArc();
        }

        // Arcs were collected destination-to-origin: restore the correct order.
        Collections.reverse(arcs);

        Path solutionPath =
                Path.concatenate(new Path(graph, arcs), comebackSolution.getPath());

        return new ShortestPathSolution(data, Status.OPTIMAL, solutionPath);
    }

}
