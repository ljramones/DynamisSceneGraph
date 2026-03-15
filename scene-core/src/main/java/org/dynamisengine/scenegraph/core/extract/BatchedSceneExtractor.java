package org.dynamisengine.scenegraph.core.extract;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.dynamisengine.scenegraph.api.extract.BatchedRenderScene;
import org.dynamisengine.scenegraph.api.extract.InstanceBatch;
import org.dynamisengine.scenegraph.api.extract.RenderKey;
import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.vectrix.core.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BatchedSceneExtractor implements SceneExtractor<BatchedRenderScene> {
    @Override
    public BatchedRenderScene extract(DefaultSceneGraph graph) {
        Map<RenderKey, MutableBatch> grouped = new LinkedHashMap<>();
        for (DefaultSceneGraph.NodeView view : graph.viewsSortedById()) {
            if (!view.visible() || !view.hasFullRenderableBinding()) {
                continue;
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
