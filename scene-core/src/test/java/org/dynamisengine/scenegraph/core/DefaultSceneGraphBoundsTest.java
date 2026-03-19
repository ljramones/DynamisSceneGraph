package org.dynamisengine.scenegraph.core;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.dynamisengine.scenegraph.api.value.BoundingSphere;
import org.junit.jupiter.api.Test;
import org.dynamisengine.vectrix.affine.Transformf;
import org.dynamisengine.vectrix.core.Vector3f;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultSceneGraphBoundsTest {

    @Test
    void setLocalBoundsSphereProducesWorldBoundsAfterExtract() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId node = graph.createNode();
        graph.setLocalBoundsSphere(node, new Vector3f(0f, 0f, 0f), 2f);

        graph.extract();
        BoundingSphere world = graph.getWorldBoundsSphere(node);

        assertNotNull(world);
        assertEquals(2f, world.radius(), 0.0001f);
    }

    @Test
    void parentTransformMovesChildWorldBoundsCenter() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId parent = graph.createNode();
        SceneNodeId child = graph.createNode();
        graph.setParent(child, parent);
        graph.setLocalBoundsSphere(child, new Vector3f(0f, 0f, 0f), 1f);

        Transformf parentTransform = new Transformf();
        parentTransform.translation.set(10f, 0f, 0f);
        graph.setLocalTransform(parent, parentTransform);

        graph.extract();
        BoundingSphere childWorld = graph.getWorldBoundsSphere(child);

        assertNotNull(childWorld);
        assertEquals(10f, childWorld.center().x(), 0.0001f);
        assertEquals(0f, childWorld.center().y(), 0.0001f);
        assertEquals(0f, childWorld.center().z(), 0.0001f);
    }

    @Test
    void queryRadiusReturnsOverlappingNodes() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId near = graph.createNode();
        SceneNodeId far = graph.createNode();

        graph.setLocalBoundsSphere(near, new Vector3f(0f, 0f, 0f), 1f);
        graph.setLocalBoundsSphere(far, new Vector3f(10f, 0f, 0f), 1f);
        graph.extract();

        var hits = graph.queryRadius(new Vector3f(0f, 0f, 0f), 2f);
        assertEquals(1, hits.size());
        assertEquals(near, hits.getFirst());
    }

    @Test
    void raycastCoarseReturnsNearestVisibleHit() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId near = graph.createNode();
        SceneNodeId far = graph.createNode();

        graph.setLocalBoundsSphere(near, new Vector3f(2f, 0f, 0f), 0.5f);
        graph.setLocalBoundsSphere(far, new Vector3f(5f, 0f, 0f), 0.5f);
        graph.extract();

        Optional<SceneNodeId> hit = graph.raycastCoarse(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f), 100f);
        assertTrue(hit.isPresent());
        assertEquals(near, hit.get());
    }
}
