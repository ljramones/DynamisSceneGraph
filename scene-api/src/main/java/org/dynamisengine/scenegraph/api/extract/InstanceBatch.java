package org.dynamisengine.scenegraph.api.extract;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.vectrix.core.Matrix4f;

import java.util.List;

public record InstanceBatch(
        RenderKey key,
        List<SceneNodeId> nodeIds,
        List<Matrix4f> worldMatrices
) {
    public InstanceBatch {
        if (nodeIds.size() != worldMatrices.size()) {
            throw new IllegalArgumentException("nodeIds and worldMatrices must have same size");
        }
    }

    public int instanceCount() {
        return worldMatrices.size();
    }
}
