package org.dynamisscenegraph.core.cull;

import org.dynamisscenegraph.core.DefaultSceneGraph;
import org.junit.jupiter.api.Test;
import org.vectrix.affine.Transformf;
import org.vectrix.core.Matrix4f;
import org.vectrix.core.Vector3f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrustumSphereCullerTest {

    @Test
    void cullsFarBoundsOutsideFrustum() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        var nodeId = graph.createNode();

        graph.setLocalBoundsSphere(nodeId, new Vector3f(0f, 0f, 0f), 0.5f);
        Transformf far = new Transformf();
        far.translation.set(10_000f, 0f, 0f);
        graph.setLocalTransform(nodeId, far);

        var view = graph.viewsSortedById().getFirst();
        var culler = new FrustumSphereCuller();
        var context = new CullingContext(new Matrix4f().identity());

        assertFalse(culler.visible(view, context));
    }

    @Test
    void includesNodesWithNoBounds() {
        DefaultSceneGraph graph = new DefaultSceneGraph();
        graph.createNode();

        var view = graph.viewsSortedById().getFirst();
        var culler = new FrustumSphereCuller();
        var context = new CullingContext(new Matrix4f().identity());

        assertTrue(culler.visible(view, context));
    }
}
