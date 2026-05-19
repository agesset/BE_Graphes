package org.insa.graphs.algorithm.shortestpath;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

public class LabelStar  extends Label{

    //estimated cost for the LabelStar
    private final double estimatedCost;

    public LabelStar(Node currentNode, boolean marked, double cost, Arc predecessorArc) {
        super(currentNode, marked, cost, predecessorArc);
        this.estimatedCost = 0.0;
    }

    @Override
    public double getTotalCost() {
        return this.estimatedCost + this.getCost();
    }
    @Override
    public double getEstimatedCost() {
        return this.estimatedCost;
    }

}

