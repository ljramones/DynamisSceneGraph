package org.dynamisscenegraph.core.cull;

import org.vectrix.core.Matrix4f;

public record CullingContext(Matrix4f viewProj) {
    public CullingContext {
        viewProj = new Matrix4f(viewProj);
    }
}
