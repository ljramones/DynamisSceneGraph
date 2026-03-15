package org.dynamisengine.scenegraph.core.cull;

import org.dynamisengine.scenegraph.core.DefaultSceneGraph;

import java.util.List;

public final class CompositeCuller implements Culler {
    private final List<Culler> cullers;

    public CompositeCuller(List<Culler> cullers) {
        this.cullers = List.copyOf(cullers);
    }

    @Override
    public boolean visible(DefaultSceneGraph.NodeView node, CullingContext ctx) {
        for (Culler culler : cullers) {
            if (!culler.visible(node, ctx)) {
                return false;
            }
        }
        return true;
    }
}
