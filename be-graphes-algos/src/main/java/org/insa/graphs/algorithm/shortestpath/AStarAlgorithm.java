package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Arc;
import org.insa.graphs.model.GraphStatistics;
import org.insa.graphs.model.Node;

public class AStarAlgorithm extends DijkstraAlgorithm {

    public AStarAlgorithm(ShortestPathData data) {
        super(data);
    }

    public Label newLabel(Node currentNode, boolean marked, double cost, Arc predecessorArc) {
        if (getInputData().getMode() == ShortestPathData.Mode.LENGTH) {
            return new Label(currentNode, marked, cost, getInputData().getOrigin().getPoint().distanceTo(currentNode.getPoint()), predecessorArc);
        } 
        else if (getInputData().getMode() == ShortestPathData.Mode.TIME) {
            
            if (getInputData().getMaximumSpeed() == GraphStatistics.NO_MAXIMUM_SPEED) {
                return new Label(currentNode, marked, cost, (getInputData().getOrigin().getPoint().distanceTo(currentNode.getPoint())), predecessorArc);
            }
            return new Label(currentNode, marked, cost, (getInputData().getOrigin().getPoint().distanceTo(currentNode.getPoint()) / getInputData().getMaximumSpeed()), predecessorArc);
        }
        throw new IllegalArgumentException("Unsupported mode: " + getInputData().getMode());
    }

}