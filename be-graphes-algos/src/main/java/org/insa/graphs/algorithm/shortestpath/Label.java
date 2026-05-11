package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

public class Label implements Comparable<Label> {
    
    private Node currentNode;
    private boolean marked;
    private double cost;
    private Arc predecessorArc;
    
    public Label(Node currentNode, boolean marked, double cost, Arc predecessorArc) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.predecessorArc = predecessorArc;
    }

    public Label(boolean marked, double cost, Arc predecessorArc) {
        currentNode = null;
        this.marked = marked;
        this.cost = cost;
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
        return Double.compare(this.getCost(), other.getCost());
    }

    public void mark() {
        this.setMarked(true);
    }

}
