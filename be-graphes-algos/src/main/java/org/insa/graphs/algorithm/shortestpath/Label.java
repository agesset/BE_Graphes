package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

public class Label {
    
    private Node current_node;
    private boolean marked;
    private int cost;
    private int marked_cost;
    private Arc predecessor_arc;
    
    public Label(Node current_node, boolean marked, int cost, int marked_cost, Arc predecessor_arc) {
        this.current_node = current_node;
        this.marked = marked;
        this.cost = cost;
        this.marked_cost = marked_cost;
        this.predecessor_arc = predecessor_arc;
    }

    public Label(boolean marked, int cost, int marked_cost, Arc predecessor_arc) {
        current_node = null;
        this.marked = marked;
        this.cost = cost;
        this.marked_cost = marked_cost;
        this.predecessor_arc = predecessor_arc;
    }

    public Node getCurrent_node() {
        return current_node;
    }

    public void setCurrent_node(Node current_node) {
        this.current_node = current_node;
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

    public int getMarked_cost() {
        return marked_cost;
    }

    public void setMarked_cost(int marked_cost) {
        this.marked_cost = marked_cost;
    }

    public Arc getPredecessor_arc() {
        return predecessor_arc;
    }

    public void setPredecessor_arc(Arc predecessor_arc) {
        this.predecessor_arc = predecessor_arc;
    }

}
