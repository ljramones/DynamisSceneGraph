# DynamisSceneGraph

Spatial hierarchy and extraction layer for the Dynamis ecosystem.

DynamisSceneGraph owns authoritative scene hierarchy, transform propagation, bounds, culling, and renderer-agnostic extraction. It does not own rendering, GPU passes, or ECS storage.

## Guides

- [API Guide](docs/API_GUIDE.md)
- [Integration Guide](docs/INTEGRATION_GUIDE.md)

## Ownership Boundaries

- Identity comes from DynamisCore: `org.dynamis.core.entity.EntityId`.
- Math comes from Vectrix: `Transformf`, `Vector3f`, `Matrix4f`.
- SceneGraph owns hierarchy + extraction only.
- LightEngine consumes extracted data and performs renderer/GPU-specific optimizations.

## Module Layout

- `scene-api`: contracts and DTOs (`SceneGraph`, `SceneNodeId`, `RenderScene`, batched extraction DTOs).
- `scene-core`: implementation (`DefaultSceneGraph`) with hierarchy, dirty propagation, bounds, culling, queries, and batching.
- `scene-runtime`: reserved for runtime helpers/integration utilities.

## Current Capabilities

Implemented in `scene-core`:

- Node creation and parent/child hierarchy with cycle prevention.
- Local `Transformf` + world `Matrix4f` propagation with dirty subtree updates.
- Local/world bounding spheres.
- CPU frustum culling (`extractCulled`).
- Spatial queries: radius overlap and coarse raycast.
- Flat extraction: `RenderScene`.
- Deterministic batched extraction: `BatchedRenderScene` grouped by `(meshHandle, materialKey)`.

Determinism rule:
- extraction iterates nodes sorted by `SceneNodeId.value()`.

## Build & Test

Requirements:

- Java 25 (`.java-version` is `25`)
- Maven 3.9+

Commands:

```bash
mvn validate
mvn compile
mvn test
```

## Versioning & Compatibility

- `scene-api` is the stable contract surface. Breaking changes should be intentional and versioned.
- `scene-core` is implementation-focused and may evolve faster (especially extension methods on `DefaultSceneGraph`).
- Renderer adapters should depend primarily on `scene-api` DTOs (`RenderScene`, `BatchedRenderScene`, `RenderKey`, `InstanceBatch`) and treat `scene-core` methods as integration-level utilities.
- Runtime baseline is Java 25 + Maven 3.9+.
- Current build expects local resolution of:
  - `org.dynamis:dynamis-core:1.0.0`
  - `org.vectrix:vectrix:1.10.13`

## Quick Usage Example

```java
DefaultSceneGraph graph = new DefaultSceneGraph();
SceneNodeId node = graph.createNode();

Transformf t = new Transformf();
t.translation.set(0f, 0f, 5f);
graph.setLocalTransform(node, t);

graph.setLocalBoundsSphere(node, new Vector3f(0f, 0f, 0f), 1f);
graph.bindRenderable(node, 1, "mat-default");

RenderScene flat = graph.extract();
BatchedRenderScene batched = graph.extractBatched();
```

## Integration Pattern

Recommended runtime flow:

1. SceneGraph produces `BatchedRenderScene`.
2. Adapter maps `RenderKey.meshHandle` + matrices to renderer ingestion APIs (for example, instance-batch registration/update).
3. Renderer performs pass/GPU-level culling and submission.

This keeps SceneGraph renderer-agnostic and prevents hierarchy duplication in rendering modules.

## Status

Completed phases:

- Multi-module scaffold
- API contracts
- Core hierarchy/transform extraction
- Bounds/culling/queries
- Batched extraction
- Host-sample integration spike in DynamisLightEngine
