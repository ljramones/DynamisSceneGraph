package org.dynamisengine.scenegraph.api;

import org.dynamisengine.vectrix.affine.Transformf;
import org.dynamisengine.vectrix.core.Matrix4f;

import java.util.List;

public interface SceneNode {

    SceneNodeId id();

    Transformf localTransform();

    Matrix4f worldMatrix();

    List<SceneNodeId> children();

    boolean visible();
}
