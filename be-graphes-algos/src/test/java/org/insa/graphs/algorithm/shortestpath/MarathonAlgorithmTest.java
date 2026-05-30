package org.insa.graphs.algorithm.shortestpath;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.insa.graphs.algorithm.ArcInspector;
import org.insa.graphs.algorithm.ArcInspectorFactory;
import org.insa.graphs.algorithm.AbstractSolution.Status;
import org.insa.graphs.model.Arc;
import org.insa.graphs.model.Graph;
import org.insa.graphs.model.Node;
import org.insa.graphs.model.Path;
import org.insa.graphs.model.io.BinaryGraphReader;
import org.insa.graphs.model.io.GraphReader;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit tests for {@link MarathonAlgorithm}.
 * <p>
 * Four test groups:
 * </p>
 * <ol>
 * <li>Mode rejection: TIME mode must throw {@link IllegalArgumentException}.</li>
 * <li>Graph too small: no 42 km circuit exists on the INSA campus map (tested for both
 * {@code PEDESTRIAN_LENGTH} and {@code LENGTH} modes).</li>
 * <li>Early termination: when d(origin, destination) &gt; 42 245 m the algorithm breaks
 * on the very first extraction.</li>
 * <li>Valid circuit on {@code haute-garonne.mapgr}: circuit length, closure, path
 * validity and arc-inspector compliance.</li>
 * </ol>
 * <p>
 * Groups 3 and 4 require the large map file to be present at the path stored in
 * {@link #LARGE_MAP}; the corresponding tests are <em>skipped</em> (not failed) when
 * the file is absent.
 * </p>
 */
public class MarathonAlgorithmTest {

    private static final double MARATHON = 42195.0;
    private static final double MARGIN = 50.0;

    private static final String LARGE_MAP =
            "/home/flo/Documents/INSA/3MIC_S2/BE-Graphes/maps/haute-garonne.mapgr";

    // Set once in @BeforeClass; null when the large map is unavailable.
    private static Graph largeGraph;

    // Computed lazily (once) in the first Groupe-4 test that needs it.
    private static ShortestPathSolution largeSolutionPedestrian;
    private static boolean largeSolutionComputed = false;

    @BeforeClass
    public static void loadLargeGraph() {
        File mapFile = new File(LARGE_MAP);
        if (!mapFile.exists()) {
            System.err.println("[MarathonAlgorithmTest] " + LARGE_MAP
                    + " not found – groups 3 & 4 will be skipped.");
            return;
        }
        try {
            largeGraph = loadGraph(mapFile);
        }
        catch (IOException e) {
            System.err
                    .println("[MarathonAlgorithmTest] Could not load large map: " + e);
        }
    }

    /**
     * Runs the marathon algorithm (PEDESTRIAN_LENGTH) on the large graph exactly once
     * and caches the result. Must be called at the start of every Groupe-4 test so that
     * the expensive computation does not block Groupe-3 tests.
     */
    private static void ensureLargeSolution() {
        if (largeSolutionComputed)
            return;
        largeSolutionComputed = true;
        if (largeGraph == null)
            return;

        Node origin = largeGraph.get(largeGraph.size() / 3);
        ArcInspector pedestrian = ArcInspectorFactory.getAllFilters().get(3);
        ShortestPathData data =
                new ShortestPathData(largeGraph, origin, origin, pedestrian);
        largeSolutionPedestrian =
                (ShortestPathSolution) new MarathonAlgorithm(data).run();
    }

    // -------------------------------------------------------------------------
    // Groupe 1 : Rejet du mode TIME
    // -------------------------------------------------------------------------

    /**
     * TIME mode is not supported: the algorithm must throw
     * {@link IllegalArgumentException} before performing any graph exploration.
     */
    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenModeIsTime() {
        Graph g = SmallGraphs.diamond();
        ArcInspector time = ArcInspectorFactory.getAllFilters().get(2);
        Node origin = g.get(0);
        ShortestPathData data = new ShortestPathData(g, origin, origin, time);
        new MarathonAlgorithm(data).run();
    }

    // -------------------------------------------------------------------------
    // Groupe 2 : Graphe trop petit — aucun circuit de 42 km possible
    // -------------------------------------------------------------------------

    /**
     * The INSA campus map covers roughly 1 km²; a 42 km pedestrian circuit is
     * structurally impossible, so the algorithm must return INFEASIBLE.
     */
    @Test
    public void infeasibleOnSmallGraphPedestrian() throws IOException {
        Graph g = loadResourceGraph("/insa.mapgr");
        ArcInspector pedestrian = ArcInspectorFactory.getAllFilters().get(3);
        Node origin = g.get(0);
        ShortestPathData data = new ShortestPathData(g, origin, origin, pedestrian);
        ShortestPathSolution sol =
                (ShortestPathSolution) new MarathonAlgorithm(data).run();
        assertEquals(Status.INFEASIBLE, sol.getStatus());
    }

    /**
     * Same as above with the LENGTH (all-roads) inspector, exercising the LENGTH mode
     * path added alongside PEDESTRIAN_LENGTH.
     */
    @Test
    public void infeasibleOnSmallGraphLength() throws IOException {
        Graph g = loadResourceGraph("/insa.mapgr");
        ArcInspector length = ArcInspectorFactory.getAllFilters().get(0);
        Node origin = g.get(0);
        ShortestPathData data = new ShortestPathData(g, origin, origin, length);
        ShortestPathSolution sol =
                (ShortestPathSolution) new MarathonAlgorithm(data).run();
        assertEquals(Status.INFEASIBLE, sol.getStatus());
    }

    // -------------------------------------------------------------------------
    // Groupe 3 : Terminaison anticipée — d(origin, destination) > 42 245 m
    // -------------------------------------------------------------------------

    /**
     * When the straight-line distance from origin to destination already exceeds
     * {@code marathonLength + errorMargin} (42 245 m), the early-exit condition fires
     * on the very first node extraction and the algorithm returns INFEASIBLE without
     * exploring the graph further.
     */
    @Test
    public void infeasibleWhenOriginTooFarFromDestination() {
        assumeTrue("Large map not loaded", largeGraph != null);

        Node origin = largeGraph.get(0);
        Node farDest = null;
        for (Node n : largeGraph.getNodes()) {
            if (origin.getPoint().distanceTo(n.getPoint()) > MARATHON + MARGIN) {
                farDest = n;
                break;
            }
        }
        assumeTrue("No graph node found farther than " + (MARATHON + MARGIN)
                + " m from node 0", farDest != null);

        ArcInspector pedestrian = ArcInspectorFactory.getAllFilters().get(3);
        ShortestPathData data =
                new ShortestPathData(largeGraph, origin, farDest, pedestrian);
        ShortestPathSolution sol =
                (ShortestPathSolution) new MarathonAlgorithm(data).run();
        assertEquals(Status.INFEASIBLE, sol.getStatus());
    }

    // -------------------------------------------------------------------------
    // Groupe 4 : Circuit valide sur grande carte
    // -------------------------------------------------------------------------

    /** A pedestrian marathon circuit must be found on the Haute-Garonne map. */
    @Test
    public void optimalStatusOnLargeMapPedestrian() {
        assumeTrue("Large map not loaded", largeGraph != null);
        ensureLargeSolution();
        assumeTrue("Pedestrian solution not computed", largeSolutionPedestrian != null);
        assertEquals(Status.OPTIMAL, largeSolutionPedestrian.getStatus());
    }

    /** The circuit total length must be within ±50 m of the marathon distance. */
    @Test
    public void circuitLengthIsMarathon() {
        assumeTrue("Large map not loaded", largeGraph != null);
        ensureLargeSolution();
        assumeTrue("No optimal pedestrian solution available",
                largeSolutionPedestrian != null
                        && largeSolutionPedestrian.getStatus() == Status.OPTIMAL);
        assertEquals(MARATHON, largeSolutionPedestrian.getPath().getLength(), MARGIN);
    }

    /** The path must be a closed loop: its first and last node are identical. */
    @Test
    public void circuitIsClosed() {
        assumeTrue("Large map not loaded", largeGraph != null);
        ensureLargeSolution();
        assumeTrue("No optimal pedestrian solution available",
                largeSolutionPedestrian != null
                        && largeSolutionPedestrian.getStatus() == Status.OPTIMAL);
        Path path = largeSolutionPedestrian.getPath();
        assertEquals(path.getOrigin(), path.getDestination());
    }

    /** Path.isValid() must return true (arcs chain consistently). */
    @Test
    public void circuitPathIsValid() {
        assumeTrue("Large map not loaded", largeGraph != null);
        ensureLargeSolution();
        assumeTrue("No optimal pedestrian solution available",
                largeSolutionPedestrian != null
                        && largeSolutionPedestrian.getStatus() == Status.OPTIMAL);
        assertTrue(largeSolutionPedestrian.getPath().isValid());
    }

    /**
     * Every arc in the pedestrian circuit must be allowed by the pedestrian arc
     * inspector, ensuring the outward search and comeback both respect the access
     * filter.
     */
    @Test
    public void circuitArcsAreAllowedByPedestrian() {
        assumeTrue("Large map not loaded", largeGraph != null);
        ensureLargeSolution();
        assumeTrue("No optimal pedestrian solution available",
                largeSolutionPedestrian != null
                        && largeSolutionPedestrian.getStatus() == Status.OPTIMAL);
        ArcInspector pedestrian = ArcInspectorFactory.getAllFilters().get(3);
        for (Arc arc : largeSolutionPedestrian.getPath().getArcs()) {
            assertTrue("Arc not allowed for pedestrians: " + arc,
                    pedestrian.isAllowed(arc));
        }
    }

    // -------------------------------------------------------------------------
    // Graph-loading helpers (mirrors ScenarioProvider private methods)
    // -------------------------------------------------------------------------

    private static Graph loadGraph(File file) throws IOException {
        try (GraphReader reader = new BinaryGraphReader(new DataInputStream(
                new BufferedInputStream(new FileInputStream(file))))) {
            return reader.read();
        }
    }

    private static Graph loadResourceGraph(String resource) throws IOException {
        InputStream in = MarathonAlgorithmTest.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("test resource not found: " + resource);
        }
        try (GraphReader reader = new BinaryGraphReader(
                new DataInputStream(new BufferedInputStream(in)))) {
            return reader.read();
        }
    }
}
