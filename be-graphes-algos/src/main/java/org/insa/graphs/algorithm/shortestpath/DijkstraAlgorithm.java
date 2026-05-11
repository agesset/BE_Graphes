package org.insa.graphs.algorithm.shortestpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.algorithm.utils.BinaryHeap;
import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Graph;
import org.insa.graphs.model.Path;

public class DijkstraAlgorithm extends ShortestPathAlgorithm {

    public DijkstraAlgorithm(ShortestPathData data) {
        super(data);
    }

    @Override
    protected ShortestPathSolution doRun() {

        // retrieve data from the input problem (getInputData() is inherited from the
        // parent class ShortestPathAlgorithm)
        final ShortestPathData data = getInputData();

        // Retrieve the graph.
        Graph graph = data.getGraph();
        final int nbNodes = graph.size();

        if (data.getOrigin().equals(data.getDestination())) {
            notifyOriginProcessed(data.getOrigin());
            notifyNodeMarked(data.getOrigin());
            notifyDestinationReached(data.getDestination());
            return new ShortestPathSolution(data, Status.OPTIMAL,
                    new Path(graph, data.getOrigin()));
        }

        // Initialize array of labels
        Label[] labels = new Label[nbNodes];
        Arrays.fill(labels, null);
        labels[data.getOrigin().getId()] = new Label(data.getOrigin(), false, 0.0, null);

        // Initialize heap
        BinaryHeap<Label> heap = new BinaryHeap<Label>();
        heap.insert(labels[data.getOrigin().getId()]);

        // Notify observers about the first event (origin processed).
        notifyOriginProcessed(data.getOrigin());

        // Actual algorithm, we will assume the graph does not contain negative cost
        boolean found = false;

        while (!found && !heap.isEmpty()) {

            Label currentLabel = heap.deleteMin();
            currentLabel.mark();

            notifyNodeMarked(currentLabel.getCurrentNode());

            if (currentLabel.getCurrentNode().equals(data.getDestination())) {
                found = true;
                break;
            }

            for (Arc successor : currentLabel.getCurrentNode().getSuccessors()) {

                // Small test to check allowed roads...
                if (!data.isAllowed(successor)) {
                    continue;
                }

                double newCost = currentLabel.getCost() + data.getCost(successor);
                Label successorLabel = labels[successor.getDestination().getId()];

                if (successorLabel == null) {
                    successorLabel = new Label(
                            successor.getDestination(),
                            false,
                            newCost,
                            successor);

                    labels[successor.getDestination().getId()] = successorLabel;
                    heap.insert(successorLabel);

                    notifyNodeReached(successor.getDestination());
                }
                else if (!successorLabel.isMarked() && successorLabel.getCost() > newCost) {
                    heap.remove(successorLabel);
                    successorLabel.setCost(newCost);
                    successorLabel.setPredecessorArc(successor);
                    heap.insert(successorLabel);
                }
            }
        }

        // variable that will contain the solution of the shortest path problem
        ShortestPathSolution solution = null;

        // Destination has no predecessor, the solution is infeasible...
        if (labels[data.getDestination().getId()] == null
                || !labels[data.getDestination().getId()].isMarked()) {
            solution = new ShortestPathSolution(data, Status.INFEASIBLE);
        } else {
            notifyDestinationReached(data.getDestination());

            // Create the path from the array of predecessors...
            ArrayList<Arc> arcs = new ArrayList<>();
            Arc arc = labels[data.getDestination().getId()].getPredecessorArc();
            while (arc != null) {
                arcs.add(arc);
                arc = labels[arc.getOrigin().getId()].getPredecessorArc();
            }

            // Reverse the path...
            Collections.reverse(arcs);

            // Create the final solution.
            solution = new ShortestPathSolution(data, Status.OPTIMAL,
                    new Path(graph, arcs));
        }

        // when the algorithm terminates, return the solution that has been found
        return solution;
    }
}