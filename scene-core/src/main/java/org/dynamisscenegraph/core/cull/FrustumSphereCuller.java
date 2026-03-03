package org.dynamisscenegraph.core.cull;

import org.dynamisscenegraph.api.value.BoundingSphere;
import org.dynamisscenegraph.core.DefaultSceneGraph;
import org.vectrix.core.Vector3f;

public final class FrustumSphereCuller implements Culler {

    @Override
    public boolean visible(DefaultSceneGraph.NodeView node, CullingContext ctx) {
        BoundingSphere bounds = node.worldBounds();
        if (bounds == null) {
            return true;
        }

        Vector3f center = bounds.center();
        return ctx.viewProj().testSphere(center.x(), center.y(), center.z(), bounds.radius());
    }
}
