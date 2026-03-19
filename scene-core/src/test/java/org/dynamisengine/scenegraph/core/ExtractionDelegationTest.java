package org.dynamisengine.scenegraph.core;

import org.dynamisengine.scenegraph.core.extract.BatchedCulledSceneExtractor;
import org.dynamisengine.scenegraph.core.extract.BatchedSceneExtractor;
import org.junit.jupiter.api.Test;
import org.dynamisengine.vectrix.affine.Transformf;
import org.dynamisengine.vectrix.core.Matrix4f;
import org.dynamisengine.vectrix.core.Vector3f;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ExtractionDelegationTest {

    @Test
    void batchedDelegatesToExtractor() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var a = graph.createNode();
        var b = graph.createNode();

        graph.bindRenderable(a, "mesh1", "mat1");
        graph.bindRenderable(b, "mesh1", "mat1");

        var delegated = graph.extractBatched();
        var direct = new BatchedSceneExtractor().extract(graph);

        assertEquals(direct.batches(), delegated.batches());
    }

    @Test
    void batchedCulledDelegatesToExtractor() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var near = graph.createNode();
        var far = graph.createNode();

        graph.bindRenderable(near, "mesh", "mat");
        graph.bindRenderable(far, "mesh", "mat");

        graph.setLocalBoundsSphere(near, new Vector3f(0f, 0f, 0f), 0.5f);
        graph.setLocalBoundsSphere(far, new Vector3f(0f, 0f, 0f), 0.5f);

        Transformf farTransform = new Transformf();
        farTransform.translation.set(10_000f, 0f, 0f);
        graph.setLocalTransform(far, farTransform);

        Matrix4f viewProj = new Matrix4f().identity();

        var delegated = graph.extractBatchedCulled(viewProj);
        var direct = new BatchedCulledSceneExtractor(viewProj).extract(graph);

        assertEquals(direct.batches(), delegated.batches());
    }
}
