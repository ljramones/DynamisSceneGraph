package org.dynamisengine.scenegraph.core.query;

import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.junit.jupiter.api.Test;
import org.vectrix.core.Vector3f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NaiveQueryEngineTest {

    @Test
    void raycastReturnsNearestHitWithDistanceInRange() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var near = graph.createNode();
        var far = graph.createNode();

        graph.setLocalBoundsSphere(near, new Vector3f(2f, 0f, 0f), 0.5f);
        graph.setLocalBoundsSphere(far, new Vector3f(5f, 0f, 0f), 0.5f);

        NaiveQueryEngine engine = new NaiveQueryEngine();
        var hit = engine.raycastCoarse(graph, new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f), 100f);

        assertTrue(hit.isPresent());
        assertEquals(near, hit.get().nodeId());
        assertTrue(hit.get().t() >= 0f && hit.get().t() <= 100f);
    }
}
