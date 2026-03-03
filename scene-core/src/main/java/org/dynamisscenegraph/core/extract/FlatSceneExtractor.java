package org.dynamisscenegraph.core.extract;

import org.dynamisscenegraph.api.RenderItem;
import org.dynamisscenegraph.api.RenderScene;
import org.dynamisscenegraph.core.DefaultSceneGraph;

import java.util.ArrayList;
import java.util.List;

public final class FlatSceneExtractor implements SceneExtractor<RenderScene> {
    @Override
    public RenderScene extract(DefaultSceneGraph graph) {
        List<RenderItem> items = new ArrayList<>();
        for (DefaultSceneGraph.NodeView view : graph.viewsInStorageOrder()) {
            if (!view.visible()) {
                continue;
            }
            if (!view.hasAnyRenderableBinding()) {
                continue;
            }
            items.add(new RenderItem(
                    view.id(),
                    view.worldMatrix(),
                    view.meshHandle(),
                    view.materialKey()
            ));
        }
        return new RenderScene(List.copyOf(items));
    }
}
