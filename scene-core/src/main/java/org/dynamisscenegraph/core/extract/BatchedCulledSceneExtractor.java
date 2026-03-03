package org.dynamisscenegraph.core.extract;

import org.dynamisscenegraph.api.SceneNodeId;
import org.dynamisscenegraph.api.extract.BatchedRenderScene;
import org.dynamisscenegraph.api.extract.InstanceBatch;
import org.dynamisscenegraph.api.extract.RenderKey;
import org.dynamisscenegraph.api.value.BoundingSphere;
import org.dynamisscenegraph.core.DefaultSceneGraph;
import org.vectrix.core.Matrix4f;
import org.vectrix.core.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BatchedCulledSceneExtractor implements SceneExtractor<BatchedRenderScene> {
    private final Matrix4f viewProj;

    public BatchedCulledSceneExtractor(Matrix4f viewProj) {
        this.viewProj = new Matrix4f(viewProj);
    }

    @Override
    public BatchedRenderScene extract(DefaultSceneGraph graph) {
        Map<RenderKey, MutableBatch> grouped = new LinkedHashMap<>();
        for (DefaultSceneGraph.NodeView view : graph.viewsSortedById()) {
            if (!view.visible() || !view.hasFullRenderableBinding()) {
                continue;
            }

            BoundingSphere bounds = view.worldBounds();
            if (bounds != null) {
                Vector3f center = bounds.center();
                if (!viewProj.testSphere(center.x(), center.y(), center.z(), bounds.radius())) {
                    continue;
                }
            }

            RenderKey key = RenderKey.of(view.meshHandle(), view.materialKey());
            MutableBatch batch = grouped.computeIfAbsent(key, ignored -> new MutableBatch());
            batch.nodeIds.add(view.id());
            batch.worldMatrices.add(view.worldMatrix());
        }

        List<InstanceBatch> batches = new ArrayList<>();
        for (Map.Entry<RenderKey, MutableBatch> entry : grouped.entrySet()) {
            MutableBatch batch = entry.getValue();
            batches.add(new InstanceBatch(
                    entry.getKey(),
                    List.copyOf(batch.nodeIds),
                    List.copyOf(batch.worldMatrices)
            ));
        }
        return new BatchedRenderScene(List.copyOf(batches));
    }

    private static final class MutableBatch {
        private final List<SceneNodeId> nodeIds = new ArrayList<>();
        private final List<Matrix4f> worldMatrices = new ArrayList<>();
    }
}
