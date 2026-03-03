package org.dynamisscenegraph.api;

import org.vectrix.affine.Transformf;
import org.vectrix.core.Matrix4f;

import java.util.List;

public interface SceneNode {

    SceneNodeId id();

    Transformf localTransform();

    Matrix4f worldMatrix();

    List<SceneNodeId> children();

    boolean visible();
}
