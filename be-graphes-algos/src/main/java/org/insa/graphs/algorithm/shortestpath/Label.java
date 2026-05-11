package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

public class Label implements Comparable<Label> {
    
    private Node currentNode;
    private boolean marked;
    private int cost;
    private int markedCost;
    private Arc predecessorArc;
    
    public Label(Node currentNode, boolean marked, int cost, int markedCost, Arc predecessorArc) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.markedCost = markedCost;
        this.predecessorArc = predecessorArc;
    }

    public Label(boolean marked, int cost, int markedCost, Arc predecessorArc) {
        currentNode = null;
        this.marked = marked;
        this.cost = cost;
        this.markedCost = markedCost;
        this.predecessorArc = predecessorArc;
    }

    public Node getcurrentNode() {
        return currentNode;
    }

    public void setcurrentNode(Node currentNode) {
        this.currentNode = currentNode;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getmarkedCost() {
        return markedCost;
    }

    public void setmarkedCost(int markedCost) {
        this.markedCost = markedCost;
    }

    public Arc getpredecessorArc() {
        return predecessorArc;
    }

    public void setpredecessorArc(Arc predecessorArc) {
        this.predecessorArc = predecessorArc;
    }

    @Override
    public int compareTo(Label other) {

        // Null safety check
        if (other == null) {
            throw new NullPointerException("Cannot compare Label with null");
        }

        // Safe comparison (avoids integer overflow)
        return Integer.compare(this.getCost(), other.getCost());
    }

    public void mark() {
        this.setMarked(true);
    }

}
