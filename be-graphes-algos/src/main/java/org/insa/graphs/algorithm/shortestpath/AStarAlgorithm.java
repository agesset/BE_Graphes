package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Point;

public class AStarAlgorithm extends DijkstraAlgorithm {

    public AStarAlgorithm(ShortestPathData data) {
        super(data);
    }

    public Label newLabel(Node currentNode, boolean marked, double cost, Arc predecessorArc, Point origin) {
        return new Label(currentNode, marked, cost, Point.distance(origin, currentNode.getPoint()), predecessorArc);
    }

}