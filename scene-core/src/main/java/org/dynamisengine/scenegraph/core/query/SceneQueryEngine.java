package org.dynamisengine.scenegraph.core.query;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.dynamisengine.vectrix.core.Vector3f;

import java.util.List;
import java.util.Optional;

public interface SceneQueryEngine {
    List<SceneNodeId> queryRadius(DefaultSceneGraph graph, Vector3f center, float radius);

    Optional<RaycastHit> raycastCoarse(DefaultSceneGraph graph, Vector3f origin, Vector3f dir, float maxDist);
}
