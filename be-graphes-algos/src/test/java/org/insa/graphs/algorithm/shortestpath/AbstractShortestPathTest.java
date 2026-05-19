package org.insa.graphs.algorithm.shortestpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Path;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Generic, reusable validation of a shortest-path algorithm.
 * <p>
 * This class is abstract and parameterised over every {@link Scenario}; it holds
 * <em>all</em> the checks. A concrete subclass only provides the algorithm to test
 * through {@link #newAlgorithm(ShortestPathData)} &mdash; exactly the pattern of
 * {@code PriorityQueueTest} / {@code BinaryHeapTest} already in this project. This is
 * how the same suite is reused for Dijkstra and for A* without copy-pasting.
 */
@RunWith(Parameterized.class)
public abstract class AbstractShortestPathTest {

    /** One JUnit run per scenario. */
    @Parameters(name = "{0}")
    public static Collection<Object> scenarios() {
        return new ArrayList<Object>(ScenarioProvider.all());
    }

    /** Scenario injected by the {@link Parameterized} runner. */
    @Parameter
    public Scenario scenario;

    /**
     * Factory implemented by each concrete subclass.
     *
     * @param data Input data for the algorithm.
     * @return A new instance of the algorithm under test.
     */
    protected abstract ShortestPathAlgorithm newAlgorithm(ShortestPathData data);

    /** Run the algorithm under test on the given data. */
    private ShortestPathSolution solve(ShortestPathData data) {
        return newAlgorithm(data).run();
    }

    /**
     * The reported status must match what the scenario expects (a feasible scenario
     * yields {@code OPTIMAL}, an infeasible one {@code INFEASIBLE}).
     */
    @Test
    public void statusMatchesExpectation() {
        ShortestPathSolution solution = solve(scenario.getData());
        assertEquals("unexpected solution status", scenario.getExpectedStatus(),
                solution.getStatus());
    }

    /**
     * On a feasible scenario the built path must be valid (arcs chained correctly) and
     * actually go from the requested origin to the requested destination.
     */
    @Test
    public void feasiblePathIsValid() {
        Assume.assumeTrue(scenario.getExpectedStatus() == Status.OPTIMAL);
        ShortestPathData data = scenario.getData();
        ShortestPathSolution solution = solve(data);

        assertTrue("expected a feasible solution", solution.isFeasible());
        Path path = solution.getPath();
        assertNotNull("a feasible solution must carry a path", path);
        assertTrue("the path must be valid (Path.isValid())", path.isValid());
        assertEquals("path must start at the origin", data.getOrigin(),
                path.getOrigin());
        if (!path.getArcs().isEmpty()) {
            assertEquals("path must end at the destination", data.getDestination(),
                    path.getDestination());
        }
    }

    /**
     * A trip whose origin equals its destination: the path is a single node, has no arc
     * and a length of zero.
     */
    @Test
    public void zeroLengthPath() {
        Assume.assumeTrue(scenario.isZeroLength());
        ShortestPathSolution solution = solve(scenario.getData());

        assertTrue("a zero-length trip must be feasible", solution.isFeasible());
        Path path = solution.getPath();
        assertTrue(path.isValid());
        assertEquals("a zero-length path has a single node", 1, path.size());
        assertTrue("a zero-length path has no arc", path.getArcs().isEmpty());
        assertEquals("a zero-length path has length 0", 0.0, path.getLength(), 1e-9);
    }

    /**
     * The cost summed by the algorithm along its path must equal the cost recomputed
     * independently by {@link Path} &mdash; compared with a tolerance, never with
     * {@code ==}, since the two accumulations differ in {@code float}/{@code double}
     * precision.
     */
    @Test
    public void costConsistency() {
        Assume.assumeTrue(scenario.getCostMetric() != Scenario.CostMetric.NONE);
        Assume.assumeTrue(scenario.getExpectedStatus() == Status.OPTIMAL);
        ShortestPathData data = scenario.getData();
        ShortestPathSolution solution = solve(data);
        Assume.assumeTrue(solution.isFeasible());

        Path path = solution.getPath();
        double algorithmCost = ShortestPathAsserts.costAlongPath(data, path);
        double pathCost = scenario.getCostMetric() == Scenario.CostMetric.LENGTH
                ? path.getLength()
                : path.getMinimumTravelTime();
        ShortestPathAsserts.assertCostEquals(
                "cost summed by the algorithm vs cost recomputed by Path", pathCost,
                algorithmCost);
    }

    /**
     * On the small scenarios, Bellman-Ford is affordable and gives a reference. What
     * must match is the <em>feasibility</em> and the <em>total cost</em> &mdash;
     * <b>not</b> the path itself, since several optimal paths of equal cost may exist.
     * The zero-length scenario is excluded: Bellman-Ford reports it as infeasible (it
     * has no predecessor arc).
     */
    @Test
    public void matchesBellmanFord() {
        Assume.assumeTrue(scenario.isSmall());
        Assume.assumeFalse(scenario.isZeroLength());
        ShortestPathData data = scenario.getData();

        ShortestPathSolution tested = solve(data);
        ShortestPathSolution reference = new BellmanFordAlgorithm(data).run();

        assertEquals("feasibility must agree with Bellman-Ford", reference.isFeasible(),
                tested.isFeasible());
        if (reference.isFeasible()) {
            assertTrue("tested path must be valid", tested.getPath().isValid());
            assertTrue("Bellman-Ford path must be valid",
                    reference.getPath().isValid());
            ShortestPathAsserts.assertCostEquals(
                    "total cost must equal the Bellman-Ford cost",
                    ShortestPathAsserts.costAlongPath(data, reference.getPath()),
                    ShortestPathAsserts.costAlongPath(data, tested.getPath()));
        }
    }

    /**
     * Correctness check that scales to the large maps, where Bellman-Ford is unusable:
     * every prefix of a shortest path is itself a shortest path. We cut the optimal
     * path at a middle node and verify that re-solving from the origin to that node
     * yields the same cost as the prefix.
     */
    @Test
    public void subpathOptimality() {
        Assume.assumeTrue(scenario.getExpectedStatus() == Status.OPTIMAL);
        ShortestPathData data = scenario.getData();
        ShortestPathSolution solution = solve(data);
        Assume.assumeTrue(solution.isFeasible());

        List<Arc> arcs = solution.getPath().getArcs();
        Assume.assumeTrue("need at least two arcs to cut the path", arcs.size() >= 2);

        int cut = arcs.size() / 2;
        Node midNode = arcs.get(cut - 1).getDestination();
        double prefixCost = 0.0;
        for (int i = 0; i < cut; i++) {
            prefixCost += data.getCost(arcs.get(i));
        }

        ShortestPathData subData = new ShortestPathData(data.getGraph(),
                data.getOrigin(), midNode, scenario.getInspector());
        ShortestPathSolution subSolution = solve(subData);

        assertTrue("origin -> middle node must be feasible", subSolution.isFeasible());
        ShortestPathAsserts.assertCostEquals(
                "a prefix of a shortest path must be a shortest path", prefixCost,
                ShortestPathAsserts.costAlongPath(subData, subSolution.getPath()));
    }

    /**
     * On the hand-built graphs the optimal cost is known exactly; the algorithm must
     * find it.
     */
    @Test
    public void knownOptimalCost() {
        Assume.assumeTrue(scenario.getKnownCost() != null);
        ShortestPathData data = scenario.getData();
        ShortestPathSolution solution = solve(data);

        assertTrue("scenario with a known cost must be feasible",
                solution.isFeasible());
        ShortestPathAsserts.assertCostEquals("known optimal cost",
                scenario.getKnownCost(),
                ShortestPathAsserts.costAlongPath(data, solution.getPath()));
    }

    /**
     * On the large maps, the optimal cost must not exceed the cost of a known real
     * reference path (the bundled {@code .path} file).
     */
    @Test
    public void notWorseThanReference() {
        Assume.assumeTrue(scenario.getReferenceUpperBound() != null);
        ShortestPathData data = scenario.getData();
        ShortestPathSolution solution = solve(data);

        assertTrue("scenario with a reference path must be feasible",
                solution.isFeasible());
        double cost = ShortestPathAsserts.costAlongPath(data, solution.getPath());
        double bound = scenario.getReferenceUpperBound();
        assertTrue("optimal cost " + cost + " must not exceed reference cost " + bound,
                cost <= bound + ShortestPathAsserts.tolerance(bound));
    }
}
