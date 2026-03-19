package org.dynamisengine.scenegraph.api;

import org.dynamisengine.vectrix.core.Matrix4f;

public record RenderItem(
        SceneNodeId nodeId,
        Matrix4f worldMatrix,
        Object meshHandle,
        Object materialKey
) {
}
