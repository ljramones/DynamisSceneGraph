# Integration Guide

This guide describes how to integrate DynamisSceneGraph with a renderer without leaking hierarchy concerns into render code.

## Target Architecture

1. Author/update nodes in `DefaultSceneGraph`.
2. Extract renderer-facing data (`extractBatched` or `extractBatchedCulled`).
3. Map batches to renderer instance ingestion.
4. Let renderer/GPU passes do pass-level optimizations.

## Recommended Adapter Pattern

Keep adapter code outside `scene-core` (for example in host app or bridge module).

Adapter responsibilities:

- Own a `DefaultSceneGraph` instance.
- Enforce `meshHandle` mapping to renderer mesh IDs.
- Convert `Matrix4f` to renderer matrix array format.
- Register/update/remove instance batches per `RenderKey`.

Do not put:

- frame graph logic in SceneGraph,
- hierarchy/transform ownership in renderer modules.

## Example Flow (per frame)

```java
BatchedRenderScene scene = graph.extractBatched();
for (InstanceBatch batch : scene.batches()) {
    int meshHandle = (Integer) batch.key().meshHandle();
    float[][] matrices = toRendererMatrices(batch.worldMatrices());
    upsertRendererBatch(meshHandle, batch.key(), matrices);
}
removeStaleRendererBatches(scene);
```

## Culling Strategy

Two valid configurations:

- CPU + GPU: use `extractBatchedCulled(viewProj)` then renderer GPU culling.
- GPU-only prefilter: use `extractBatched()` and rely on renderer culling.

Start with GPU-only if camera matrix plumbing is not ready, then switch to CPU+GPU.

## Data Contracts

- `RenderKey(meshHandle, materialKey)` is renderer-agnostic and non-null.
- `InstanceBatch.instanceCount()` equals `nodeIds.size()` and `worldMatrices.size()`.
- For LightEngine spikes, `meshHandle` should be `Integer`.

## Validation Checklist

- Extraction order is deterministic.
- Batch lifecycle is correct (register once, update thereafter, remove stale).
- Matrix payload shape matches renderer expectations (`[n][16]`).
- SceneGraph remains free of renderer-specific imports.
