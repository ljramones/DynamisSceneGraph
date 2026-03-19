package org.dynamisengine.scenegraph.core.cull;

import org.dynamisengine.vectrix.core.Matrix4f;

public record CullingContext(Matrix4f viewProj) {
    public CullingContext {
        viewProj = new Matrix4f(viewProj);
    }
}
