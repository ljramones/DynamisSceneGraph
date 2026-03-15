package org.dynamisengine.scenegraph.core;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.junit.jupiter.api.Test;
import org.vectrix.affine.Transformf;
import org.vectrix.core.Vector3f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultSceneGraphTest {

    @Test
    void createNodeReturnsUniquePositiveIds() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId a = graph.createNode();
        SceneNodeId b = graph.createNode();

        assertNotEquals(a, b);
        assertTrue(a.value() > 0);
        assertTrue(b.value() > 0);
    }

    @Test
    void parentingRejectsCycles() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId a = graph.createNode();
        SceneNodeId b = graph.createNode();
        SceneNodeId c = graph.createNode();

        graph.setParent(b, a);
        graph.setParent(c, b);

        assertThrows(IllegalArgumentException.class, () -> graph.setParent(a, c));
    }

    @Test
    void extractIncludesOnlyBoundRenderables() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId a = graph.createNode();
        graph.createNode();

        graph.bindRenderable(a, "meshA", "matA");

        var scene = graph.extract();
        assertEquals(1, scene.items().size());
        assertEquals(a, scene.items().getFirst().nodeId());
    }

    @Test
    void setLocalTransformMarksSubtreeDirtyAndUpdatesWorld() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId parent = graph.createNode();
        SceneNodeId child = graph.createNode();
        graph.setParent(child, parent);
        graph.bindRenderable(child, "mesh", "mat");

        Transformf transform = new Transformf();
        transform.translation.set(new Vector3f(10f, 0f, 0f));
        graph.setLocalTransform(parent, transform);

        var scene = graph.extract();
        assertEquals(1, scene.items().size());
        assertNotNull(scene.items().getFirst().worldMatrix());
    }
}
