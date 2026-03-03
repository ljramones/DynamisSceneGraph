package org.dynamisscenegraph.core;

import org.dynamisscenegraph.api.RenderScene;
import org.dynamisscenegraph.api.SceneGraph;
import org.dynamisscenegraph.api.SceneNode;
import org.dynamisscenegraph.api.SceneNodeId;
import org.dynamisscenegraph.api.extract.BatchedRenderScene;
import org.dynamisscenegraph.api.value.BoundingSphere;
import org.dynamisscenegraph.core.extract.BatchedCulledSceneExtractor;
import org.dynamisscenegraph.core.extract.BatchedSceneExtractor;
import org.dynamisscenegraph.core.extract.FlatCulledSceneExtractor;
import org.dynamisscenegraph.core.extract.FlatSceneExtractor;
import org.dynamisscenegraph.core.query.NaiveQueryEngine;
import org.dynamisscenegraph.core.query.SceneQueryEngine;
import org.vectrix.affine.Transformf;
import org.vectrix.core.Matrix4f;
import org.vectrix.core.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default SceneGraph implementation (v1).
 *
 * Owns:
 * - node hierarchy (parent/children)
 * - local transforms (Transformf)
 * - world matrices (Matrix4f)
 * - dirty propagation for transforms
 * - renderer-agnostic extraction (RenderScene)
 */
public final class DefaultSceneGraph implements SceneGraph {

    private final Map<SceneNodeId, Node> nodes = new HashMap<>();
    private final SceneQueryEngine queryEngine = new NaiveQueryEngine();
    private long nextId = 1;

    @Override
    public SceneNodeId createNode() {
        SceneNodeId id = SceneNodeId.of(this.nextId++);
        this.nodes.put(id, new Node(id));
        return id;
    }

    @Override
    public void setParent(SceneNodeId childId, SceneNodeId parentId) {
        Node child = requireNode(childId);
        Node parent = requireNode(parentId);

        if (childId.equals(parentId)) {
            throw new IllegalArgumentException("A node cannot be parented to itself: " + childId);
        }
        if (isAncestor(childId, parentId)) {
            throw new IllegalArgumentException("Cycle detected: " + parentId + " is a descendant of " + childId);
        }

        if (child.parent != null) {
            requireNode(child.parent).children.remove(childId);
        }

        child.parent = parentId;
        parent.children.add(childId);
        markDirtySubtree(childId);
    }

    @Override
    public void setLocalTransform(SceneNodeId nodeId, Transformf transform) {
        Node node = requireNode(nodeId);
        node.local.set(transform);
        markDirtySubtree(nodeId);
    }

    @Override
    public Optional<SceneNode> getNode(SceneNodeId nodeId) {
        Node node = this.nodes.get(nodeId);
        return node == null ? Optional.empty() : Optional.of(node.view());
    }

    /**
     * Binds renderer-opaque render data to a node.
     * Nodes without bindings are omitted from extraction methods.
     */
    public void bindRenderable(SceneNodeId nodeId, Object meshHandle, Object materialKey) {
        Node node = requireNode(nodeId);
        node.meshHandle = meshHandle;
        node.materialKey = materialKey;
    }

    public void unbindRenderable(SceneNodeId nodeId) {
        Node node = requireNode(nodeId);
        node.meshHandle = null;
        node.materialKey = null;
    }

    public void setLocalBoundsSphere(SceneNodeId nodeId, Vector3f center, float radius) {
        Node node = requireNode(nodeId);
        node.localBounds = BoundingSphere.of(center, radius);
        markDirtySubtree(nodeId);
    }

    public BoundingSphere getWorldBoundsSphere(SceneNodeId nodeId) {
        ensureWorldUpdated();
        Node node = requireNode(nodeId);
        return copyBounds(node.worldBounds);
    }

    @Override
    public RenderScene extract() {
        return new FlatSceneExtractor().extract(this);
    }

    public RenderScene extractCulled(Matrix4f viewProj) {
        return new FlatCulledSceneExtractor(viewProj).extract(this);
    }

    public BatchedRenderScene extractBatched() {
        return new BatchedSceneExtractor().extract(this);
    }

    public BatchedRenderScene extractBatchedCulled(Matrix4f viewProj) {
        return new BatchedCulledSceneExtractor(viewProj).extract(this);
    }

    public List<SceneNodeId> queryRadius(Vector3f center, float radius) {
        if (radius < 0f) {
            throw new IllegalArgumentException("radius must be >= 0, got: " + radius);
        }
        return queryEngine.queryRadius(this, center, radius);
    }

    public Optional<SceneNodeId> raycastCoarse(Vector3f origin, Vector3f dir, float maxDist) {
        if (maxDist < 0f) {
            throw new IllegalArgumentException("maxDist must be >= 0, got: " + maxDist);
        }

        Vector3f direction = new Vector3f(dir);
        if (direction.lengthSquared() == 0f) {
            throw new IllegalArgumentException("dir must be non-zero");
        }
        direction.normalize();
        return queryEngine.raycastCoarse(this, origin, direction, maxDist).map(hit -> hit.nodeId());
    }

    // Internal extraction/query accessors used by helper pipelines.
    public void ensureWorldUpdated() {
        updateWorldMatrices();
    }

    public List<NodeView> viewsInStorageOrder() {
        ensureWorldUpdated();
        List<NodeView> views = new ArrayList<>(this.nodes.size());
        for (Node node : this.nodes.values()) {
            views.add(node.viewSnapshot());
        }
        return List.copyOf(views);
    }

    public List<NodeView> viewsSortedById() {
        ensureWorldUpdated();
        List<NodeView> views = new ArrayList<>(this.nodes.size());
        for (Node node : this.nodes.values()) {
            views.add(node.viewSnapshot());
        }
        views.sort(Comparator.comparingLong(v -> v.id().value()));
        return List.copyOf(views);
    }

    private void updateWorldMatrices() {
        for (Node node : this.nodes.values()) {
            if (node.parent == null) {
                updateSubtree(node, null);
            }
        }
    }

    private void updateSubtree(Node node, Matrix4f parentWorld) {
        if (node.dirty) {
            Matrix4f localMatrix = toMatrix(node.local);
            if (parentWorld == null) {
                node.world.set(localMatrix);
            } else {
                parentWorld.mul(localMatrix, node.world);
            }
            if (node.localBounds != null) {
                node.worldBounds = transformSphere(node.localBounds, node.world);
            } else {
                node.worldBounds = null;
            }
            node.dirty = false;
        }

        for (SceneNodeId childId : node.children) {
            updateSubtree(requireNode(childId), node.world);
        }
    }

    private static Matrix4f toMatrix(Transformf transform) {
        return new Matrix4f().translationRotateScale(
                transform.translation,
                transform.rotation,
                transform.scale
        );
    }

    private static BoundingSphere transformSphere(BoundingSphere localBounds, Matrix4f world) {
        Vector3f worldCenter = new Vector3f(localBounds.center()).mulPosition(world);
        float worldRadius = localBounds.radius() * maxScale(world);
        return BoundingSphere.of(worldCenter, worldRadius);
    }

    private static float maxScale(Matrix4f world) {
        float sx = new Vector3f(1f, 0f, 0f).mulDirection(world).length();
        float sy = new Vector3f(0f, 1f, 0f).mulDirection(world).length();
        float sz = new Vector3f(0f, 0f, 1f).mulDirection(world).length();
        return Math.max(sx, Math.max(sy, sz));
    }

    private void markDirtySubtree(SceneNodeId rootId) {
        Deque<SceneNodeId> stack = new ArrayDeque<>();
        stack.push(rootId);

        while (!stack.isEmpty()) {
            SceneNodeId id = stack.pop();
            Node node = requireNode(id);
            node.dirty = true;
            for (SceneNodeId childId : node.children) {
                stack.push(childId);
            }
        }
    }

    private boolean isAncestor(SceneNodeId potentialAncestor, SceneNodeId nodeId) {
        SceneNodeId parentId = requireNode(nodeId).parent;
        while (parentId != null) {
            if (parentId.equals(potentialAncestor)) {
                return true;
            }
            parentId = requireNode(parentId).parent;
        }
        return false;
    }

    private Node requireNode(SceneNodeId id) {
        Node node = this.nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node: " + id);
        }
        return node;
    }

    private static BoundingSphere copyBounds(BoundingSphere bounds) {
        if (bounds == null) {
            return null;
        }
        return BoundingSphere.of(bounds.center(), bounds.radius());
    }

    public static final class NodeView {
        private final SceneNodeId id;
        private final boolean visible;
        private final Matrix4f worldMatrix;
        private final BoundingSphere worldBounds;
        private final Object meshHandle;
        private final Object materialKey;

        private NodeView(
                SceneNodeId id,
                boolean visible,
                Matrix4f worldMatrix,
                BoundingSphere worldBounds,
                Object meshHandle,
                Object materialKey
        ) {
            this.id = id;
            this.visible = visible;
            this.worldMatrix = worldMatrix;
            this.worldBounds = worldBounds;
            this.meshHandle = meshHandle;
            this.materialKey = materialKey;
        }

        public SceneNodeId id() {
            return id;
        }

        public boolean visible() {
            return visible;
        }

        public Matrix4f worldMatrix() {
            return new Matrix4f(worldMatrix);
        }

        public BoundingSphere worldBounds() {
            return copyBounds(worldBounds);
        }

        public Object meshHandle() {
            return meshHandle;
        }

        public Object materialKey() {
            return materialKey;
        }

        public boolean hasAnyRenderableBinding() {
            return meshHandle != null || materialKey != null;
        }

        public boolean hasFullRenderableBinding() {
            return meshHandle != null && materialKey != null;
        }
    }

    private static final class Node {
        private final SceneNodeId id;
        private SceneNodeId parent;
        private final List<SceneNodeId> children = new ArrayList<>();
        private boolean visible = true;
        private final Transformf local = new Transformf();
        private final Matrix4f world = new Matrix4f();
        private BoundingSphere localBounds;
        private BoundingSphere worldBounds;
        private boolean dirty = true;
        private Object meshHandle;
        private Object materialKey;

        private Node(SceneNodeId id) {
            this.id = id;
        }

        private NodeView viewSnapshot() {
            return new NodeView(
                    id,
                    visible,
                    new Matrix4f(world),
                    copyBounds(worldBounds),
                    meshHandle,
                    materialKey
            );
        }

        private SceneNode view() {
            return new SceneNode() {
                @Override
                public SceneNodeId id() {
                    return id;
                }

                @Override
                public Transformf localTransform() {
                    return new Transformf(local);
                }

                @Override
                public Matrix4f worldMatrix() {
                    return new Matrix4f(world);
                }

                @Override
                public List<SceneNodeId> children() {
                    return List.copyOf(children);
                }

                @Override
                public boolean visible() {
                    return visible;
                }
            };
        }
    }
}
