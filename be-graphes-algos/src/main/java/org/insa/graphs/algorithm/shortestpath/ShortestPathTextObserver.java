package org.insa.graphs.algorithm.shortestpath;

import java.io.PrintStream;

import org.insa.graphs.model.Node;

public class ShortestPathTextObserver implements ShortestPathObserver {

    private final PrintStream stream;

    public ShortestPathTextObserver(PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public void notifyOriginProcessed(Node node) {
        stream.println("Origin Node " + node.getId() + " processed.");
    }

    @Override
    public void notifyNodeReached(Node node) {
        stream.println("Node " + node.getId() + " reached.");
    }

    @Override
    public void notifyNodeMarked(Node node) {
        stream.println("Node " + node.getId() + " marked.");
    }

    @Override
    public void notifyDestinationReached(Node node) {
        stream.println("Destination Node " + node.getId() + " reached.");
    }

}
