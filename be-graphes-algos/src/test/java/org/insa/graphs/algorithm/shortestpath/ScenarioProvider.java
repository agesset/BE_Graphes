package org.insa.graphs.algorithm.shortestpath;

import static org.insa.graphs.algorithm.shortestpath.Scenario.CostMetric.LENGTH;
import static org.insa.graphs.algorithm.shortestpath.Scenario.CostMetric.MIN_TRAVEL_TIME;
import static org.insa.graphs.algorithm.shortestpath.Scenario.CostMetric.NONE;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.algorithm.ArcInspector;
import org.insa.graphs.algorithm.ArcInspectorFactory;
import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Graph;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Path;
import org.insa.graphs.model.io.BinaryGraphReader;
import org.insa.graphs.model.io.BinaryPathReader;
import org.insa.graphs.model.io.GraphReader;
import org.insa.graphs.model.io.PathReader;

/**
 * Builds the list of {@link Scenario}s exercised by the shortest-path tests.
 * <p>
 * Three families of scenarios:
 * <ul>
 * <li><b>Hand-built graphs</b> ({@link SmallGraphs}) &mdash; controlled cases with
 * known optimal costs: a real trip, a zero-length trip and an infeasible trip.</li>
 * <li><b>insa.mapgr</b> (test resource) &mdash; a real but small road map; short and
 * long trips, in distance and in time, with the all-roads, cars and pedestrian
 * inspectors. Small enough for Bellman-Ford.</li>
 * <li><b>haute-garonne.mapgr</b> &mdash; a large road map; only added when the file is
 * present locally. Endpoints are taken from the bundled reference {@code .path} file,
 * whose cost also serves as an upper bound.</li>
 * </ul>
 * The result is memoised; graphs are loaded at most once per JVM.
 */
final class ScenarioProvider {

    /** Folder holding the large maps (outside the repository). */
    private static final String MAPS_DIR =
            "/home/flo/Documents/INSA/3MIC_S2/BE-Graphes/maps/";

    private static final String HAUTE_GARONNE_MAP = MAPS_DIR + "haute-garonne.mapgr";
    private static final String HAUTE_GARONNE_PATH =
            MAPS_DIR + "path_fr31_insa_bikini_canal.path";

    private static List<Scenario> cached;

    private ScenarioProvider() {
        // Utility class.
    }

    /**
     * @return The (memoised) list of all test scenarios.
     */
    static synchronized List<Scenario> all() {
        if (cached == null) {
            List<Scenario> scenarios = new ArrayList<>();
            addSmallGraphScenarios(scenarios);
            addInsaScenarios(scenarios);
            addHauteGaronneScenarios(scenarios);
            cached = scenarios;
        }
        return cached;
    }

    // ------------------------------------------------------------------
    // Hand-built graphs
    // ------------------------------------------------------------------

    private static void addSmallGraphScenarios(List<Scenario> out) {
        ArcInspector length = ArcInspectorFactory.getAllFilters().get(0);

        Graph diamond = SmallGraphs.diamond();
        out.add(new Scenario.Builder("small/diamond 0->4 (optimal trip)",
                new ShortestPathData(diamond, diamond.get(0), diamond.get(4), length),
                length).small(true).knownCost(6.0).costMetric(LENGTH).build());
        out.add(new Scenario.Builder("small/diamond 0->0 (zero-length trip)",
                new ShortestPathData(diamond, diamond.get(0), diamond.get(0), length),
                length).small(true).knownCost(0.0).costMetric(LENGTH).build());
        out.add(new Scenario.Builder("small/diamond 4->0 (no path)",
                new ShortestPathData(diamond, diamond.get(4), diamond.get(0), length),
                length).small(true).expectedStatus(Status.INFEASIBLE).build());

        Graph twoEqual = SmallGraphs.twoEqualPaths();
        out.add(new Scenario.Builder("small/two-equal 0->3 (two optimal paths)",
                new ShortestPathData(twoEqual, twoEqual.get(0), twoEqual.get(3),
                        length),
                length).small(true).knownCost(5.0).costMetric(LENGTH).build());
    }

    // ------------------------------------------------------------------
    // insa.mapgr (small real road map, from test resources)
    // ------------------------------------------------------------------

    private static void addInsaScenarios(List<Scenario> out) {
        Graph insa;
        try {
            insa = loadResourceGraph("/insa.mapgr");
        }
        catch (Exception e) {
            System.err.println("[ScenarioProvider] insa.mapgr unavailable, "
                    + "skipping insa scenarios: " + e);
            return;
        }

        List<ArcInspector> filters = ArcInspectorFactory.getAllFilters();
        ArcInspector length = filters.get(0); // shortest path, all roads
        ArcInspector carTime = filters.get(2); // fastest path, all roads
        ArcInspector pedestrian = filters.get(3); // fastest path, pedestrian
        Node origin = insa.get(0);

        // Distance: a short trip and a long one.
        List<Node> reachable = reachableInBfsOrder(insa, origin, length);
        if (reachable.size() >= 3) {
            Node shortDestination = reachable.get(Math.max(1, reachable.size() / 12));
            Node longDestination = reachable.get(reachable.size() - 1);
            out.add(new Scenario.Builder("insa/short trip (distance)",
                    new ShortestPathData(insa, origin, shortDestination, length),
                    length).small(true).costMetric(LENGTH).build());
            out.add(new Scenario.Builder("insa/long trip (distance)",
                    new ShortestPathData(insa, origin, longDestination, length), length)
                    .small(true).costMetric(LENGTH).build());
        }

        // A zero-length trip on a real map.
        out.add(new Scenario.Builder("insa/zero-length trip (distance)",
                new ShortestPathData(insa, origin, origin, length), length).small(true)
                .knownCost(0.0).costMetric(LENGTH).build());

        // Time, by car.
        List<Node> reachableByCar = reachableInBfsOrder(insa, origin, carTime);
        if (reachableByCar.size() >= 3) {
            out.add(new Scenario.Builder("insa/long trip by car (time)",
                    new ShortestPathData(insa, origin,
                            reachableByCar.get(reachableByCar.size() - 1), carTime),
                    carTime).small(true).costMetric(MIN_TRAVEL_TIME).build());
        }

        // Time, on foot: exercises the pedestrian (non-car) paths of the map.
        List<Node> reachableOnFoot = reachableInBfsOrder(insa, origin, pedestrian);
        if (reachableOnFoot.size() >= 3) {
            out.add(new Scenario.Builder("insa/long walk for pedestrian (time)",
                    new ShortestPathData(insa, origin,
                            reachableOnFoot.get(reachableOnFoot.size() - 1),
                            pedestrian),
                    pedestrian).small(true).costMetric(NONE).build());
        }
    }

    // ------------------------------------------------------------------
    // haute-garonne.mapgr (large real road map, optional)
    // ------------------------------------------------------------------

    private static void addHauteGaronneScenarios(List<Scenario> out) {
        File mapFile = new File(HAUTE_GARONNE_MAP);
        File pathFile = new File(HAUTE_GARONNE_PATH);
        if (!mapFile.exists() || !pathFile.exists()) {
            System.err.println("[ScenarioProvider] haute-garonne map/path not found, "
                    + "skipping large-map scenarios.");
            return;
        }

        Graph graph;
        Path reference;
        try {
            graph = loadGraph(mapFile);
            reference = loadPath(pathFile, graph);
        }
        catch (Exception e) {
            System.err.println("[ScenarioProvider] could not load haute-garonne, "
                    + "skipping large-map scenarios: " + e);
            return;
        }

        Node origin = reference.getOrigin();
        Node destination = reference.getDestination();
        List<ArcInspector> filters = ArcInspectorFactory.getAllFilters();
        ArcInspector length = filters.get(0);
        ArcInspector carTime = filters.get(2);

        // The reference path is a valid (not necessarily optimal) route, so its
        // length / travel time is an upper bound on the optimal cost.
        out.add(new Scenario.Builder("haute-garonne/long trip (distance)",
                new ShortestPathData(graph, origin, destination, length), length)
                .small(false).costMetric(LENGTH)
                .referenceUpperBound(reference.getLength()).build());
        out.add(new Scenario.Builder("haute-garonne/long trip (time)",
                new ShortestPathData(graph, origin, destination, carTime), carTime)
                .small(false).costMetric(MIN_TRAVEL_TIME)
                .referenceUpperBound(reference.getMinimumTravelTime()).build());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Breadth-first traversal from {@code origin}, following only arcs allowed by
     * {@code inspector}. Gives a feasibility ground truth independent of the algorithm
     * under test, and yields nodes ordered by hop distance (the last one is the
     * farthest, i.e. a good "long trip" destination).
     */
    private static List<Node> reachableInBfsOrder(Graph graph, Node origin,
            ArcInspector inspector) {
        boolean[] seen = new boolean[graph.size()];
        List<Node> order = new ArrayList<>();
        Deque<Node> queue = new ArrayDeque<>();
        seen[origin.getId()] = true;
        queue.add(origin);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            order.add(node);
            for (Arc arc : node.getSuccessors()) {
                if (!inspector.isAllowed(arc)) {
                    continue;
                }
                Node next = arc.getDestination();
                if (!seen[next.getId()]) {
                    seen[next.getId()] = true;
                    queue.add(next);
                }
            }
        }
        return order;
    }

    private static Graph loadResourceGraph(String resource) throws IOException {
        InputStream in = ScenarioProvider.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("test resource not found: " + resource);
        }
        try (GraphReader reader = new BinaryGraphReader(
                new DataInputStream(new BufferedInputStream(in)))) {
            return reader.read();
        }
    }

    private static Graph loadGraph(File file) throws IOException {
        try (GraphReader reader = new BinaryGraphReader(new DataInputStream(
                new BufferedInputStream(new FileInputStream(file))))) {
            return reader.read();
        }
    }

    private static Path loadPath(File file, Graph graph) throws IOException {
        try (PathReader reader = new BinaryPathReader(new DataInputStream(
                new BufferedInputStream(new FileInputStream(file))))) {
            return reader.readPath(graph);
        }
    }
}
