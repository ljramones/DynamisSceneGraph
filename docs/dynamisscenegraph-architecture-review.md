# DynamisSceneGraph Architecture Boundary Ratification Review

Date: 2026-03-09

## Intent and Scope

This is a boundary-ratification review for DynamisSceneGraph based on current repository code and docs.

This pass does not refactor code. It defines strict ownership, dependency, and API-surface constraints to reduce overlap risk with DynamisECS, DynamisWorldEngine, and DynamisLightEngine.

## 1) Repo Overview (Grounded)

Repository shape:

- Multi-module Maven project:
  - `scene-api`
  - `scene-core`
  - `scene-runtime`
- Separate `demo` aggregator with `demo-projection` (explicitly outside main reactor).

Implemented code:

- `scene-api` (`org.dynamisscenegraph.api.*`)
  - `SceneGraph`, `SceneNode`, `SceneNodeId`
  - `RenderScene`, `RenderItem`
  - batched DTOs: `BatchedRenderScene`, `InstanceBatch`, `RenderKey`
  - bounds value type: `BoundingSphere`
  - `SceneNodeMetadata` with optional `EntityId` owner reference
- `scene-core` (`org.dynamisscenegraph.core.*`)
  - `DefaultSceneGraph` hierarchy + transform + bounds + extraction + query APIs
  - extraction strategies (`Flat*`, `Batched*`)
  - culling (`FrustumSphereCuller`, `CompositeCuller`)
  - spatial queries (`NaiveQueryEngine`)
- `scene-runtime`
  - currently skeletal (no implemented source classes)

Dependency shape (main modules):

- Depends on `dynamis-core` and `vectrix`.
- No compile dependency from main modules to ECS, Session, WorldEngine, LightEngine, or Scripting.
- `demo/demo-projection` intentionally depends on ECS + Session to show projection/rebuild integration.

## 2) Strict Ownership Statement

### 2.1 What DynamisSceneGraph should exclusively own

DynamisSceneGraph should own a renderer-agnostic spatial graph substrate:

- scene-node hierarchy (parent/child relationships, cycle prevention)
- local transform storage and world-transform propagation
- scene-graph-local visibility/bounds and traversal semantics
- structural extraction DTO generation from graph state
- graph-local spatial query primitives

### 2.2 What is appropriate for SceneGraph

Appropriate concerns:

- hierarchy mutation semantics
- transform dirty propagation and world-matrix resolution
- bounds propagation/representation and coarse culling helpers
- deterministic extraction ordering and batching as transport shape

### 2.3 What DynamisSceneGraph must never own

DynamisSceneGraph must not own:

- ECS state authority or component storage ownership
- world/session authority (lifecycle, save/load policy, replication, gameplay orchestration)
- renderer/frame-graph/pass policy
- material/shader/render submission policy
- scripting policy
- content/asset authority beyond opaque references in extraction output

## 3) Dependency Rules

### 3.1 Allowed dependencies for DynamisSceneGraph

- `DynamisCore` for minimal shared identities/contracts (`EntityId` reference metadata is acceptable)
- `vectrix` for math/transform/matrix primitives
- JDK collections/math/runtime primitives

### 3.2 Forbidden dependencies for DynamisSceneGraph

- `DynamisWorldEngine` orchestration policy layers
- `DynamisSession` authority/persistence layers
- `DynamisLightEngine` render-policy and pass-planning layers
- `DynamisScripting` policy layers
- direct ownership dependencies on feature subsystems

Note: demo-only integration dependencies are acceptable when isolated outside core modules.

### 3.3 Who may depend on DynamisSceneGraph

- `DynamisLightEngine` and render-adapter layers as consumers of extracted scene data
- `DynamisWorldEngine` orchestration layers that maintain world state and project into SceneGraph
- tooling/integration modules that convert ECS/world/session data into SceneGraph nodes

Dependency direction intent:

- SceneGraph remains a spatial substrate below render/world policy layers.

## 4) Public vs Internal Boundary Assessment

### 4.1 Canonical public boundary

Primary public boundary should be `scene-api`:

- graph contracts (`SceneGraph`, `SceneNode`, IDs)
- extraction DTO contracts (`RenderScene`, `BatchedRenderScene`, related DTOs)
- value types (`BoundingSphere`)

### 4.2 Internal/implementation areas

`scene-core` should remain implementation-focused:

- `DefaultSceneGraph` internals
- extractor/culler/query engines
- mutation/storage strategy details

### 4.3 Current boundary pressure points

1. `DefaultSceneGraph` currently carries many extension methods beyond `SceneGraph` interface (`bindRenderable`, culled extraction, batched extraction, queries). This is practical but encourages direct coupling to one concrete implementation.

2. `DefaultSceneGraph.NodeView` is public and used by extractor/query internals. That class is implementation detail in spirit, but currently exposed as a concrete type.

3. `scene-runtime` is declared as an integration module but currently empty, so integration boundaries are not yet concretely encoded.

## 5) Policy Leakage / Overlap Findings

### 5.1 Major clean boundaries confirmed

- No ECS/world/session/renderer dependencies in `scene-api`/`scene-core` compile graph.
- SceneGraph currently behaves as a spatial + extraction substrate, not a world/session authority layer.
- Culling/extraction remain renderer-agnostic data preparation (no frame graph or GPU dispatch concerns).

### 5.2 Notable overlap/drift risks

1. **Render-facing DTOs in `scene-api`**  
`RenderScene`/`RenderItem`/batch DTOs are useful, but they are render-oriented. This is acceptable if treated as renderer-agnostic transport, but it is a boundary that could drift into render policy if expanded carelessly.

2. **Opaque `Object` handles in public DTOs**  
`meshHandle`/`materialKey` as `Object` preserve decoupling, but can weaken contract clarity and let renderer-specific conventions leak into graph-level call sites.

3. **`SceneNodeMetadata` includes `EntityId`**  
This is a useful ownership link, but SceneGraph must keep it as reference metadata only. ECS/world authority must remain external.

4. **Docs/process drift**  
`AGENTS.md` still states bootstrap/no build tooling, while the repo now has full Maven multi-module structure. This is documentation drift, not architectural drift, but it can mislead boundary decisions.

5. **Integration examples in demo module**  
`demo-projection` composes Session/ECS/SceneGraph correctly as projection code. Because it is isolated from main reactor, this is acceptable; however, those patterns should not migrate into `scene-core`.

## 6) Relationship Clarification

### 6.1 SceneGraph vs ECS

- ECS owns generic component/entity storage substrate.
- SceneGraph owns spatial hierarchy and transform/bounds graph.
- Projection/mapping between ECS state and SceneGraph nodes belongs in integration/runtime layers, not inside ECS or SceneGraph core.

### 6.2 SceneGraph vs WorldEngine

- WorldEngine should own orchestration and authority.
- SceneGraph should not become a hidden world-authority layer.
- SceneGraph provides spatial graph services to higher orchestration layers.

### 6.3 SceneGraph vs LightEngine

- SceneGraph provides extracted scene transport (flat/batched/culling-prepared).
- LightEngine should own render planning, pass policy, and GPU execution policy.
- SceneGraph should not absorb frame-graph/render-pass concerns.

## 7) Ratification Result

**Ratified with constraints.**

Why:

- Core dependency direction is clean and mostly one-way.
- Current functionality aligns with a spatial hierarchy substrate.
- Constraints are needed because public surface currently exposes concrete implementation paths (`DefaultSceneGraph` extensions, `NodeView`) and render-facing DTO pressure could drift into policy territory if left unchecked.

## 8) Strict Boundary Rules to Carry Forward

1. Keep `scene-api` as contract-first public surface; avoid expanding renderer policy into it.
2. Keep `scene-core` implementation details replaceable; avoid freezing `DefaultSceneGraph` internals as ecosystem contracts.
3. Keep SceneGraph authoritative for spatial structure only, not world/session authority.
4. Keep LightEngine responsible for render planning/GPU policy, consuming SceneGraph outputs as inputs.
5. Keep ECS-to-SceneGraph projection outside both cores (integration/runtime boundary).

## 9) Recommended Next Step

Next deep review should be **DynamisLightEngine**.

Reason:

- It is the nearest boundary where SceneGraph extraction contracts meet render policy/planning.
- Clarifying LightEngine ownership next will prevent render policy from leaking downward into SceneGraph or upward from GPU execution layers.
