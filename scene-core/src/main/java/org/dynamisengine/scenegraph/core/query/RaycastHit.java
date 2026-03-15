package org.dynamisengine.scenegraph.core.query;

import org.dynamisengine.scenegraph.api.SceneNodeId;

public record RaycastHit(SceneNodeId nodeId, float t) {
}
