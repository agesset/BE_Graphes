package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

public class Label implements Comparable<Label> {
    
    private Node currentNode;
    private boolean marked;
    private double cost;
    private double estimatedCost;
    private Arc predecessorArc;
    
    public Label(Node currentNode, boolean marked, double cost, Arc predecessorArc) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = 0.0;
        this.predecessorArc = predecessorArc;
    }

    public Label(boolean marked, double cost, Arc predecessorArc) {
        currentNode = null;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = 0.0;
        this.predecessorArc = predecessorArc;
    }

    public Label(Node currentNode, boolean marked, double cost, double estimatedCost, Arc predecessorArc) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = estimatedCost;
        this.predecessorArc = predecessorArc;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Node currentNode) {
        this.currentNode = currentNode;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double cost) {
        this.cost = estimatedCost;
    }

    public double getTotalCost() {
        return getCost() + getEstimatedCost();
    }

    public Arc getPredecessorArc() {
        return predecessorArc;
    }

    public void setPredecessorArc(Arc predecessorArc) {
        this.predecessorArc = predecessorArc;
    }

    @Override
    public int compareTo(Label other) {

        // Null safety check
        if (other == null) {
            throw new NullPointerException("Cannot compare Label with null");
        }

        // Safe comparison (avoids integer overflow)
        int comparedTotalCost = Double.compare(this.getTotalCost(), other.getTotalCost());
        if (comparedTotalCost != 0) {
            return comparedTotalCost;
        }
        else {
            return Double.compare(this.getEstimatedCost(), other.estimatedCost);
        }
    }

    public void mark() {
        this.setMarked(true);
    }

}