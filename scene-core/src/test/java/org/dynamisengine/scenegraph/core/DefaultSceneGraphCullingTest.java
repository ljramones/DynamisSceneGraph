package org.dynamisengine.scenegraph.core;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.junit.jupiter.api.Test;
import org.vectrix.affine.Transformf;
import org.vectrix.core.Matrix4f;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DefaultSceneGraphCullingTest {

    @Test
    void extractCulledIncludesOriginAndExcludesFarNode() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        SceneNodeId atOrigin = graph.createNode();
        SceneNodeId farAway = graph.createNode();

        graph.bindRenderable(atOrigin, "meshA", "matA");
        graph.bindRenderable(farAway, "meshB", "matB");
        graph.setLocalBoundsSphere(atOrigin, new org.vectrix.core.Vector3f(0f, 0f, 0f), 0.5f);
        graph.setLocalBoundsSphere(farAway, new org.vectrix.core.Vector3f(0f, 0f, 0f), 0.5f);

        Transformf farTransform = new Transformf();
        farTransform.translation.set(10_000f, 0f, 0f);
        graph.setLocalTransform(farAway, farTransform);

        Matrix4f viewProj = new Matrix4f().identity();
        var culled = graph.extractCulled(viewProj);

        assertEquals(1, culled.items().size());
        assertEquals(atOrigin, culled.items().getFirst().nodeId());
    }
}
