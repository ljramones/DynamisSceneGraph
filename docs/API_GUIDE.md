# API Guide

This guide covers the public contract layer (`scene-api`) and the current reference implementation (`scene-core`).

## Core Types

From `scene-api`:

- `SceneNodeId`: positive node identifier local to SceneGraph.
- `SceneNode`: read-only node view (id, local transform, world matrix, children, visibility).
- `SceneGraph`: core contract (`createNode`, `setParent`, `setLocalTransform`, `getNode`, `extract`).
- `RenderScene` / `RenderItem`: flat extraction DTOs.
- `BoundingSphere`: value type (`center`, `radius`).
- `RenderKey`, `InstanceBatch`, `BatchedRenderScene`: batched extraction DTOs.

## Default Implementation

`scene-core` provides `DefaultSceneGraph` with extra extension methods beyond `SceneGraph`:

- `bindRenderable(nodeId, meshHandle, materialKey)`
- `unbindRenderable(nodeId)`
- `setLocalBoundsSphere(nodeId, center, radius)`
- `getWorldBoundsSphere(nodeId)`
- `extractCulled(viewProj)`
- `extractBatched()`
- `extractBatchedCulled(viewProj)`
- `queryRadius(center, radius)`
- `raycastCoarse(origin, dir, maxDist)`

## Minimal Usage

```java
DefaultSceneGraph graph = new DefaultSceneGraph();
SceneNodeId root = graph.createNode();
SceneNodeId child = graph.createNode();
graph.setParent(child, root);

graph.bindRenderable(child, 1, "mat-default");
graph.setLocalBoundsSphere(child, new Vector3f(0f, 0f, 0f), 1f);

Transformf t = new Transformf();
t.translation.set(0f, 0f, 5f);
graph.setLocalTransform(root, t);

RenderScene flat = graph.extract();
BatchedRenderScene batched = graph.extractBatched();
```

## Behavior Notes

- Dirty propagation is subtree-based for reparent and transform updates.
- `extractCulled` and `extractBatchedCulled` include nodes with no bounds.
- Batched extraction is deterministic: nodes are processed by ascending `SceneNodeId.value()`.
- `RenderKey` requires non-null `meshHandle` and `materialKey`.
