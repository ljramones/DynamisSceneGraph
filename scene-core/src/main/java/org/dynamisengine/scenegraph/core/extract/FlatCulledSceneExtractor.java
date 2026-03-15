package org.dynamisengine.scenegraph.core.extract;

import org.dynamisengine.scenegraph.api.RenderItem;
import org.dynamisengine.scenegraph.api.RenderScene;
import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.dynamisengine.scenegraph.core.cull.CompositeCuller;
import org.dynamisengine.scenegraph.core.cull.CullingContext;
import org.dynamisengine.scenegraph.core.cull.Culler;
import org.dynamisengine.scenegraph.core.cull.FrustumSphereCuller;
import org.vectrix.core.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class FlatCulledSceneExtractor implements SceneExtractor<RenderScene> {
    private final Matrix4f viewProj;
    private final Culler culler;

    public FlatCulledSceneExtractor(Matrix4f viewProj) {
        this.viewProj = new Matrix4f(viewProj);
        this.culler = new CompositeCuller(List.of(new FrustumSphereCuller()));
    }

    @Override
    public RenderScene extract(DefaultSceneGraph graph) {
        CullingContext context = new CullingContext(viewProj);
        List<RenderItem> items = new ArrayList<>();
        for (DefaultSceneGraph.NodeView view : graph.viewsInStorageOrder()) {
            if (!view.visible()) {
                continue;
            }
            if (!view.hasAnyRenderableBinding()) {
                continue;
            }
            if (!culler.visible(view, context)) {
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
