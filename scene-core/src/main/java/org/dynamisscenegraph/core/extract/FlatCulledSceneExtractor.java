package org.dynamisscenegraph.core.extract;

import org.dynamisscenegraph.api.RenderItem;
import org.dynamisscenegraph.api.RenderScene;
import org.dynamisscenegraph.api.value.BoundingSphere;
import org.dynamisscenegraph.core.DefaultSceneGraph;
import org.vectrix.core.Matrix4f;
import org.vectrix.core.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class FlatCulledSceneExtractor implements SceneExtractor<RenderScene> {
    private final Matrix4f viewProj;

    public FlatCulledSceneExtractor(Matrix4f viewProj) {
        this.viewProj = new Matrix4f(viewProj);
    }

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
            BoundingSphere bounds = view.worldBounds();
            if (bounds != null) {
                Vector3f center = bounds.center();
                if (!viewProj.testSphere(center.x(), center.y(), center.z(), bounds.radius())) {
                    continue;
                }
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
