package org.dynamisengine.scenegraph.api;

import org.dynamisengine.vectrix.affine.Transformf;

import java.util.Optional;

public interface SceneGraph {

    SceneNodeId createNode();

    void setParent(SceneNodeId child, SceneNodeId parent);

    void setLocalTransform(SceneNodeId node, Transformf transform);

    Optional<SceneNode> getNode(SceneNodeId nodeId);

    RenderScene extract();
}
