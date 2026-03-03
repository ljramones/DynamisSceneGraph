package org.dynamisscenegraph.core.cull;

import org.dynamisscenegraph.core.DefaultSceneGraph;

@FunctionalInterface
public interface Culler {
    boolean visible(DefaultSceneGraph.NodeView node, CullingContext ctx);
}
