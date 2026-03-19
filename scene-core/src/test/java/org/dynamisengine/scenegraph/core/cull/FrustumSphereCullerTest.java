package org.dynamisengine.scenegraph.core.cull;

import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.junit.jupiter.api.Test;
import org.dynamisengine.vectrix.affine.Transformf;
import org.dynamisengine.vectrix.core.Matrix4f;
import org.dynamisengine.vectrix.core.Vector3f;

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
