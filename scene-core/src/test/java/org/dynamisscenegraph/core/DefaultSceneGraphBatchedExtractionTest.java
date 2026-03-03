package org.dynamisscenegraph.core;

import org.dynamisscenegraph.api.extract.RenderKey;
import org.junit.jupiter.api.Test;
import org.vectrix.affine.Transformf;
import org.vectrix.core.Matrix4f;
import org.vectrix.core.Vector3f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultSceneGraphBatchedExtractionTest {

    @Test
    void extractBatchedGroupsByRenderKey() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var a = graph.createNode();
        var b = graph.createNode();
        var c = graph.createNode();

        graph.bindRenderable(a, "mesh1", "mat1");
        graph.bindRenderable(b, "mesh1", "mat1");
        graph.bindRenderable(c, "mesh2", "mat2");

        var scene = graph.extractBatched();
        assertEquals(2, scene.batches().size());

        var batch11 = scene.batches().stream()
                .filter(batch -> batch.key().equals(RenderKey.of("mesh1", "mat1")))
                .findFirst()
                .orElseThrow();
        var batch22 = scene.batches().stream()
                .filter(batch -> batch.key().equals(RenderKey.of("mesh2", "mat2")))
                .findFirst()
                .orElseThrow();

        assertEquals(2, batch11.instanceCount());
        assertEquals(1, batch22.instanceCount());
    }

    @Test
    void extractBatchedIsDeterministicByNodeIdOrdering() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var n1 = graph.createNode();
        var n2 = graph.createNode();
        var n3 = graph.createNode();

        graph.bindRenderable(n3, "mesh", "mat");
        graph.bindRenderable(n1, "mesh", "mat");
        graph.bindRenderable(n2, "mesh", "mat");

        var scene = graph.extractBatched();
        assertEquals(1, scene.batches().size());

        var ids = scene.batches().getFirst().nodeIds();
        assertEquals(3, ids.size());
        assertEquals(n1, ids.get(0));
        assertEquals(n2, ids.get(1));
        assertEquals(n3, ids.get(2));
    }

    @Test
    void extractBatchedCulledExcludesOutsideNodes() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var near = graph.createNode();
        var far = graph.createNode();

        graph.bindRenderable(near, "mesh1", "mat1");
        graph.bindRenderable(far, "mesh1", "mat1");

        graph.setLocalBoundsSphere(near, new Vector3f(0f, 0f, 0f), 0.5f);
        graph.setLocalBoundsSphere(far, new Vector3f(0f, 0f, 0f), 0.5f);

        Transformf farTransform = new Transformf();
        farTransform.translation.set(10_000f, 0f, 0f);
        graph.setLocalTransform(far, farTransform);

        var scene = graph.extractBatchedCulled(new Matrix4f().identity());
        assertEquals(1, scene.batches().size());
        assertEquals(1, scene.batches().getFirst().instanceCount());
        assertEquals(near, scene.batches().getFirst().nodeIds().getFirst());
        assertFalse(scene.batches().getFirst().nodeIds().contains(far));
    }

    @Test
    void extractBatchedCulledIncludesNoBoundsNodes() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var node = graph.createNode();
        graph.bindRenderable(node, "mesh", "mat");

        var scene = graph.extractBatchedCulled(new Matrix4f().identity());
        assertEquals(1, scene.batches().size());
        assertTrue(scene.batches().getFirst().nodeIds().contains(node));
    }
}
