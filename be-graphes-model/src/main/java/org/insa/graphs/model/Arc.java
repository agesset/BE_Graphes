package org.insa.graphs.model;

import java.util.List;

/**
 * Abstract class representing a directed arc in the graph.
 *
 * <p>
 * {@code Arc} is abstract rather than an interface so that common behaviour
 * ({@link #getTravelTime} and {@link #getMinimumTravelTime}) can be shared, while
 * concrete subclasses ({@link ArcForward}, {@link ArcBackward}) can represent
 * two-way roads without duplicating stored data.
 * </p>
 * <p>
 * Arcs must never be instantiated directly; always use
 * {@link Node#linkNodes(Node, Node, float, RoadInformation, java.util.ArrayList)}
 * to guarantee correct pairing of forward and backward arcs.
 * </p>
 */
public abstract class Arc {

    /**
     * @return Origin node of this arc.
     */
    public abstract Node getOrigin();

    /**
     * @return Destination node of this arc.
     */
    public abstract Node getDestination();

    /**
     * @return Length of this arc, in meters.
     */
    public abstract float getLength();

    /**
     * Compute the time required to travel this arc if moving at the given speed.
     *
     * @param speed Speed to compute the travel time.
     * @return Time (in seconds) required to travel this arc at the given speed (in
     *         kilometers-per-hour).
     */
    public double getTravelTime(double speed) {
        return getLength() * 3600.0 / (speed * 1000.0);
    }

    /**
     * Compute and return the minimum time required to travel this arc, or the time
     * required to travel this arc at the maximum speed allowed.
     *
     * @return Minimum time required to travel this arc, in seconds.
     * @see Arc#getTravelTime(double)
     */
    public double getMinimumTravelTime() {
        return getTravelTime(getRoadInformation().getMaximumSpeed());
    }

    /**
     * @return Road information for this arc.
     */
    public abstract RoadInformation getRoadInformation();

    /**
     * @return Points representing segments of this arc.
     */
    public abstract List<Point> getPoints();
}
