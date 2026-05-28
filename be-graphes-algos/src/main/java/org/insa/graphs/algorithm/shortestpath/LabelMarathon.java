package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.model.Node;
import org.insa.graphs.model.Arc;

/**
 * <p>
 * Label attached to a {@link Node} while {@link MarathonAlgorithm} searches for a closed
 * itinerary whose total length is close to the marathon distance.
 * </p>
 * <p>
 * It plays the same role as {@link Label} &mdash; it records the outward cost, a
 * heuristic estimate, the predecessor arc and the marked flag &mdash; but it carries in
 * addition the target {@code marathonLength} and is ordered, not by total cost, but by
 * how close the resulting circuit is to that target (see
 * {@link #compareTo(LabelMarathon)}).
 * </p>
 */
public class LabelMarathon implements Comparable<LabelMarathon> {

    /** Node this label refers to. */
    private Node currentNode;

    /** {@code true} once this label has been extracted from the priority queue. */
    private boolean marked;

    /**
     * Length of the best known outward path from the origin to {@link #currentNode}.
     */
    private double cost;

    /**
     * Straight-line distance from {@link #currentNode} back to the destination, used as
     * the heuristic estimate of the remaining return cost.
     */
    private double estimatedCost;

    /** Arc used to reach {@link #currentNode} on the best known outward path. */
    private Arc predecessorArc;

    /** Target circuit length (in metres) the marathon itinerary must approach. */
    private double marathonLength;

    /**
     * Create a marathon label.
     *
     * @param currentNode Node this label refers to.
     * @param marked Whether the node is already marked.
     * @param cost Length of the outward path from the origin to the node.
     * @param estimatedCost Straight-line distance from the node back to the destination.
     * @param predecessorArc Arc used to reach the node, or {@code null} for the origin.
     * @param marathonLength Target circuit length the marathon itinerary must approach.
     */
    public LabelMarathon(Node currentNode, boolean marked, double cost,
            double estimatedCost, Arc predecessorArc, double marathonLength) {
        this.currentNode = currentNode;
        this.marked = marked;
        this.cost = cost;
        this.estimatedCost = estimatedCost;
        this.predecessorArc = predecessorArc;
        this.marathonLength = marathonLength;
    }

    /** @return Node this label refers to. */
    public Node getCurrentNode() {
        return currentNode;
    }

    /** @return {@code true} if this label has been marked. */
    public boolean isMarked() {
        return marked;
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
     * @return Target circuit length (in metres) the marathon itinerary must approach.
     */
    public double getMarathonLength() {
        return marathonLength;
    }

    /**
     * @return The marathon distance still missing from the optimistic out-and-back
     *         estimate, that is {@code marathonLength - (cost + estimatedCost)}. A
     *         positive value means the circuit is still shorter than the target, a
     *         negative one means it already overshoots it. This is the key used to order
     *         labels in the priority queue (see {@link #compareTo(LabelMarathon)}).
     */
    public double getNotComebackTotalCost() {
        return getMarathonLength() - (getCost() + getEstimatedCost());
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
     * Order labels so that the priority queue explores first the nodes whose circuit
     * length is closest to the marathon target.
     * <p>
     * The ordering key is {@link #getNotComebackTotalCost()}, i.e.
     * {@code marathonLength - (cost + estimatedCost)}. A label that has not yet reached
     * the target keeps a key in {@code [0, marathonLength]} &mdash; the smaller the key,
     * the closer the circuit is to completion. A label that already overshoots the
     * target has a negative key; it is remapped to {@code cost + estimatedCost} (a value
     * greater than {@code marathonLength}) so that overshooting labels are always
     * examined after the others. Ties are broken on the estimated cost, favouring labels
     * closer to the destination.
     * </p>
     *
     * @param other Label to compare this label to.
     * @return A negative integer, zero, or a positive integer as this label is
     *         respectively more, equally, or less promising than {@code other}.
     * @throws NullPointerException if {@code other} is {@code null}.
     */
    @Override
    public int compareTo(LabelMarathon other) {

        // Null safety check.
        if (other == null) {
            throw new NullPointerException("Cannot compare Label with null");
        }

        // Remaining marathon distance for each label (negative when the circuit estimate
        // already exceeds the target).
        double thisTotalCost = getNotComebackTotalCost();
        double otherTotalCost = other.getNotComebackTotalCost();

        // Overshooting labels are remapped to a value above marathonLength so that they
        // sort after every label that has not yet reached the target.
        if (thisTotalCost < 0) {
            thisTotalCost = marathonLength - thisTotalCost;
        }

        if (otherTotalCost < 0) {
            otherTotalCost = marathonLength - otherTotalCost;
        }

        // Compare on proximity to the marathon target; break ties on the heuristic
        // estimate, favouring labels closer to the destination.
        int comparedTotalCost = Double.compare(thisTotalCost, otherTotalCost);
        if (comparedTotalCost != 0) {
            return comparedTotalCost;
        }
        else {
            return Double.compare(this.getEstimatedCost(), other.estimatedCost);
        }
    }

    /** Mark this label: the shortest cost to its node is now final. */
    public void mark() {
        this.marked = true;
    }

}
