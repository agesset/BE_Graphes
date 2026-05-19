package org.insa.graphs.algorithm.shortestpath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Cross-validation of Dijkstra against A*: on every scenario, both algorithms must
 * agree on feasibility and, when feasible, on the optimal cost. Their paths may differ
 * when several optimal paths of equal cost exist, so only the cost is compared, never
 * the arcs themselves.
 */
@RunWith(Parameterized.class)
public class DijkstraVsAStarTest {

    @Parameters(name = "{0}")
    public static Collection<Object> scenarios() {
        return new ArrayList<Object>(ScenarioProvider.all());
    }

    @Parameter
    public Scenario scenario;

    @Test
    public void dijkstraAndAStarAgree() {
        ShortestPathData data = scenario.getData();

        ShortestPathSolution dijkstra = new DijkstraAlgorithm(data).run();
        ShortestPathSolution astar = new AStarAlgorithm(data).run();

        assertEquals("Dijkstra and A* must agree on feasibility", dijkstra.isFeasible(),
                astar.isFeasible());
        if (dijkstra.isFeasible()) {
            assertTrue("Dijkstra path must be valid", dijkstra.getPath().isValid());
            assertTrue("A* path must be valid", astar.getPath().isValid());
            ShortestPathAsserts.assertCostEquals(
                    "Dijkstra and A* must find the same optimal cost",
                    ShortestPathAsserts.costAlongPath(data, dijkstra.getPath()),
                    ShortestPathAsserts.costAlongPath(data, astar.getPath()));
        }
    }
}
