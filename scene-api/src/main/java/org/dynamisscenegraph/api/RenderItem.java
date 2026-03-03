package org.dynamisscenegraph.api;

import org.vectrix.core.Matrix4f;

public record RenderItem(
        SceneNodeId nodeId,
        Matrix4f worldMatrix,
        Object meshHandle,
        Object materialKey
) {
}
