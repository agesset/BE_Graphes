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

        // TODO: implement the Dijkstra algorithm
        // Retrieve the graph.
        Graph graph = data.getGraph();

        final int nbNodes = graph.size();

        // Initialize array of labels
        int currentLabelSize = 0;
        Label[] labels = new Label[nbNodes];
        Arrays.fill(labels, null);
        labels[0] = new Label(data.getOrigin(), false, 0, 0, null);
        ++currentLabelSize;

        // Initialize heap
        BinaryHeap<Label> heap = new BinaryHeap<Label>();
        heap.insert(labels[0]);

        // Notify observers about the first event (origin processed).
        notifyOriginProcessed(data.getOrigin());

        // Initialize array of predecessors.
        Arc[] predecessorArcs = new Arc[nbNodes];

        // Actual algorithm, we will assume the graph does not contain negative cost
        int markedNodes = 0;
        while (markedNodes < nbNodes) {
            Label currentLabel = heap.findMin();
            currentLabel.mark();
            ++markedNodes;

            if (markedNodes > 1) {
                predecessorArcs[currentLabel.getpredecessorArc().getOrigin().getId()] = currentLabel.getpredecessorArc();
            }

            for (Arc successor : currentLabel.getcurrentNode().getSuccessors()) {
                
                //Small test to check allowed roads...
                if (!data.isAllowed(successor)) {
                    continue;
                }

                Label successorLabel = null;
                for (int i = 0; i < nbNodes; i++) {
                    if (successor.getDestination().equals(labels[i].getcurrentNode())) {
                        successorLabel = labels[i];
                        break;
                    }
                }

                if (successorLabel == null) {
                    labels[currentLabelSize - 1] = new Label(successor.getDestination(), false, 0, 0, successor);
                }

                if (!successorLabel.isMarked()) {
                    float newCost = (currentLabel.getCost() + successor.getLength());

                    if (successorLabel.getCost() > newCost) {
                        successorLabel.setCost((int)newCost);

                        int indexOfSuccessor = heap.indexOf(successorLabel);
                        
                        if (indexOfSuccessor != -1) {
                            heap.remove(successorLabel);
                        }
                        
                        heap.insert(successorLabel);
                    }
                }
            }
        }

        // variable that will contain the solution of the shortest path problem
        ShortestPathSolution solution = null;

        // Destination has no predecessor, the solution is infeasible...
        if (predecessorArcs[data.getDestination().getId()] == null) {
            solution = new ShortestPathSolution(data, Status.INFEASIBLE);
        }
        else {

            // The destination has been found, notify the observers.
            notifyDestinationReached(data.getDestination());

            // Create the path from the array of predecessors...
            ArrayList<Arc> arcs = new ArrayList<>();
            Arc arc = predecessorArcs[data.getDestination().getId()];
            while (arc != null) {
                arcs.add(arc);
                arc = predecessorArcs[arc.getOrigin().getId()];
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
