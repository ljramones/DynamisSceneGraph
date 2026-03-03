package org.dynamisscenegraph.core.query;

import org.dynamisscenegraph.api.SceneNodeId;
import org.dynamisscenegraph.core.DefaultSceneGraph;
import org.vectrix.core.Vector3f;

import java.util.List;
import java.util.Optional;

public interface SceneQueryEngine {
    List<SceneNodeId> queryRadius(DefaultSceneGraph graph, Vector3f center, float radius);

    Optional<RaycastHit> raycastCoarse(DefaultSceneGraph graph, Vector3f origin, Vector3f dir, float maxDist);
}
