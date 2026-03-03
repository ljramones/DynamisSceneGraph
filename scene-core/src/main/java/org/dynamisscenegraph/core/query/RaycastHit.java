package org.dynamisscenegraph.core.query;

import org.dynamisscenegraph.api.SceneNodeId;

public record RaycastHit(SceneNodeId nodeId, float t) {
}
