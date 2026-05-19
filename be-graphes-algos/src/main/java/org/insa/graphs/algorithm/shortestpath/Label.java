package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

/**
 * <p>
 * Label attached to a {@link Node} while a shortest-path algorithm
 * ({@link DijkstraAlgorithm}, {@link AStarAlgorithm}) is running.
 * </p>
 * <p>
 * For the node it refers to, a label stores:
 * </p>
 * <ul>
 * <li>{@code cost} &mdash; the cost of the best path found so far from the origin to
 * that node (the <em>g</em> value);</li>
 * <li>{@code estimatedCost} &mdash; an optional heuristic estimate of the remaining
 * cost from that node to the destination (the <em>h</em> value, always {@code 0} for
 * plain Dijkstra);</li>
 * <li>{@code marked} &mdash; whether the node has been definitively marked, i.e. its
 * shortest cost is now known;</li>
 * <li>{@code predecessorArc} &mdash; the arc used to reach the node on that best path,
 * which lets the final path be rebuilt by following predecessors backwards.</li>
 * </ul>
 * <p>
 * Labels are {@link Comparable}: they are ordered by their total cost <em>f = g +
 * h</em> so that a priority queue always yields the most promising label first (see
 * {@link #compareTo(Label)}).
 * </p>
 */
public class Label implements Comparable<Label> {

    /** Node this label refers to. */
    private Node currentNode;

    /** {@code true} once the shortest cost to {@link #currentNode} is final. */
    private boolean marked;

    /** Cost of the best known path from the origin to {@link #currentNode}. */
    private double cost;

    /**
     * Heuristic estimate of the remaining cost from {@link #currentNode} to the
     * destination; always {@code 0} for Dijkstra.
     */
    private double estimatedCost;

    /** Arc used to reach {@link #currentNode} on the best known path. */
    private Arc predecessorArc;

    /**
     * Create a label without a heuristic (estimated cost set to {@code 0}); this is the
     * label used by {@link DijkstraAlgorithm}.
     *
     * @param currentNode Node this label refers to.
     * @param marked Whether the node is already marked.
     * @param cost Cost of the path from the origin to the node.
     * @param predecessorArc Arc used to reach the node, or {@code null} for the origin.
     */
    public Label(Node currentNode, boolean marked, double cost, Arc predecessorArc) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = 0.0;
        this.predecessorArc = predecessorArc;
    }

    /**
     * Create a label not attached to any node and without a heuristic.
     *
     * @param marked Whether the label is already marked.
     * @param cost Cost of the path from the origin.
     * @param predecessorArc Arc used to reach the node, or {@code null}.
     */
    public Label(boolean marked, double cost, Arc predecessorArc) {
        currentNode = null;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = 0.0;
        this.predecessorArc = predecessorArc;
    }

    /**
     * Create a label carrying a heuristic estimate; this is the label used by
     * {@link AStarAlgorithm}.
     *
     * @param currentNode Node this label refers to.
     * @param marked Whether the node is already marked.
     * @param cost Cost of the path from the origin to the node.
     * @param estimatedCost Heuristic estimate of the remaining cost to the destination.
     * @param predecessorArc Arc used to reach the node, or {@code null} for the origin.
     */
    public Label(Node currentNode, boolean marked, double cost, double estimatedCost,
            Arc predecessorArc) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = estimatedCost;
        this.predecessorArc = predecessorArc;
    }

    /** @return Node this label refers to. */
    public Node getCurrentNode() {
        return currentNode;
    }

    /**
     * @param currentNode New node for this label.
     */
    public void setCurrentNode(Node currentNode) {
        this.currentNode = currentNode;
    }

    /** @return {@code true} if this label has been marked. */
    public boolean isMarked() {
        return marked;
    }

    /**
     * @param marked New marked state for this label.
     */
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    /**
     * @return Cost of the best known path from the origin to this node (the <em>g</em>
     *         value).
     */
    public double getCost() {
        return cost;
    }

    /**
     * @param cost New cost of the best known path from the origin to this node.
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * @return Heuristic estimate of the remaining cost to the destination (the
     *         <em>h</em> value).
     */
    public double getEstimatedCost() {
        return estimatedCost;
    }

    /**
     * Set the heuristic estimate of the remaining cost to the destination.
     *
     * @param estimatedCost New estimated cost.
     */
    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    /**
     * @return Total cost <em>f = g + h</em> of this label, i.e. the cost from the
     *         origin plus the heuristic estimate. This is the key used to order labels
     *         in the priority queue.
     */
    public double getTotalCost() {
        return getCost() + getEstimatedCost();
    }

    /** @return Arc used to reach this node on the best known path. */
    public Arc getPredecessorArc() {
        return predecessorArc;
    }

    /**
     * @param predecessorArc New predecessor arc for this label.
     */
    public void setPredecessorArc(Arc predecessorArc) {
        this.predecessorArc = predecessorArc;
    }

    /**
     * Compare two labels by their total cost, so that a priority queue always returns
     * the label with the smallest <em>f = g + h</em> first. Ties are broken on the
     * estimated cost, which favours labels closer to the destination.
     *
     * @param other Label to compare this label to.
     * @return A negative integer, zero, or a positive integer as this label is
     *         respectively cheaper than, equal to, or more expensive than
     *         {@code other}.
     * @throws NullPointerException if {@code other} is {@code null}.
     */
    @Override
    public int compareTo(Label other) {

        // Null safety check.
        if (other == null) {
            throw new NullPointerException("Cannot compare Label with null");
        }

        // Compare on the total cost f = g + h; Double.compare avoids the pitfalls of
        // subtracting two doubles (overflow, NaN, signed zero).
        int comparedTotalCost =
                Double.compare(this.getTotalCost(), other.getTotalCost());
        if (comparedTotalCost != 0) {
            return comparedTotalCost;
        }
        else {
            // Equal total cost: break the tie on the heuristic estimate.
            return Double.compare(this.getEstimatedCost(), other.estimatedCost);
        }
    }

    /** Mark this label: the shortest cost to its node is now final. */
    public void mark() {
        this.setMarked(true);
    }

}
