package org.insa.graphs.algorithm.shortestpath;

import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.algorithm.ArcInspector;

/**
 * A single test scenario for a shortest-path algorithm.
 * <p>
 * A scenario bundles everything a test needs: a map (graph), a cost nature (distance /
 * time, carried by the {@link ArcInspector}), an origin and a destination
 * ({@link ShortestPathData}), the expected feasibility and, optionally, a known optimal
 * cost or a reference upper bound.
 * <p>
 * Scenarios are immutable and built through the nested {@link Builder}.
 */
public final class Scenario {

    /**
     * Which {@link org.insa.graphs.model.Path} accessor recomputes the same quantity as
     * the scenario's cost function, so that the algorithm cost can be cross-checked.
     */
    public enum CostMetric {
        /** Cost = arc length, comparable to {@code Path.getLength()}. */
        LENGTH,
        /**
         * Cost = travel time at max speed, comparable to
         * {@code Path.getMinimumTravelTime()}.
         */
        MIN_TRAVEL_TIME,
        /** No directly comparable {@code Path} accessor (e.g. pedestrian). */
        NONE
    }

    private final String name;
    private final ShortestPathData data;
    private final ArcInspector inspector;
    private final Status expectedStatus;
    private final boolean small;
    private final Double knownCost;
    private final Double referenceUpperBound;
    private final CostMetric costMetric;

    private Scenario(Builder builder) {
        this.name = builder.name;
        this.data = builder.data;
        this.inspector = builder.inspector;
        this.expectedStatus = builder.expectedStatus;
        this.small = builder.small;
        this.knownCost = builder.knownCost;
        this.referenceUpperBound = builder.referenceUpperBound;
        this.costMetric = builder.costMetric;
    }

    /** @return Human-readable name (used as the JUnit parameter label). */
    public String getName() {
        return name;
    }

    /** @return Input data (graph, origin, destination, arc inspector). */
    public ShortestPathData getData() {
        return data;
    }

    /** @return Arc inspector of this scenario (kept accessible for sub-paths). */
    public ArcInspector getInspector() {
        return inspector;
    }

    /** @return Expected solution status ({@code OPTIMAL} or {@code INFEASIBLE}). */
    public Status getExpectedStatus() {
        return expectedStatus;
    }

    /** @return {@code true} if small enough to also run Bellman-Ford. */
    public boolean isSmall() {
        return small;
    }

    /** @return Known optimal cost, or {@code null} if unknown. */
    public Double getKnownCost() {
        return knownCost;
    }

    /**
     * @return Cost of a known (not necessarily optimal) reference path, which the
     *         optimal cost must not exceed, or {@code null} if none.
     */
    public Double getReferenceUpperBound() {
        return referenceUpperBound;
    }

    /** @return Metric usable to cross-check the cost against {@code Path}. */
    public CostMetric getCostMetric() {
        return costMetric;
    }

    /** @return {@code true} if origin and destination are the same node. */
    public boolean isZeroLength() {
        return data.getOrigin().equals(data.getDestination());
    }

    @Override
    public String toString() {
        return name;
    }

    /** Fluent builder for {@link Scenario}. */
    public static final class Builder {

        private final String name;
        private final ShortestPathData data;
        private final ArcInspector inspector;
        private Status expectedStatus = Status.OPTIMAL;
        private boolean small = false;
        private Double knownCost = null;
        private Double referenceUpperBound = null;
        private CostMetric costMetric = CostMetric.NONE;

        /**
         * @param name Human-readable scenario name.
         * @param data Input data for the algorithm.
         * @param inspector Arc inspector used to build {@code data} (kept so sub-path
         *        scenarios can be rebuilt).
         */
        public Builder(String name, ShortestPathData data, ArcInspector inspector) {
            this.name = name;
            this.data = data;
            this.inspector = inspector;
        }

        public Builder expectedStatus(Status status) {
            this.expectedStatus = status;
            return this;
        }

        public Builder small(boolean value) {
            this.small = value;
            return this;
        }

        public Builder knownCost(double cost) {
            this.knownCost = cost;
            return this;
        }

        public Builder referenceUpperBound(double cost) {
            this.referenceUpperBound = cost;
            return this;
        }

        public Builder costMetric(CostMetric metric) {
            this.costMetric = metric;
            return this;
        }

        public Scenario build() {
            return new Scenario(this);
        }
    }
}
