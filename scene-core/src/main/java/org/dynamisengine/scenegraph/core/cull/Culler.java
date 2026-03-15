package org.dynamisengine.scenegraph.core.cull;

import org.dynamisengine.scenegraph.core.DefaultSceneGraph;

@FunctionalInterface
public interface Culler {
    boolean visible(DefaultSceneGraph.NodeView node, CullingContext ctx);
}
