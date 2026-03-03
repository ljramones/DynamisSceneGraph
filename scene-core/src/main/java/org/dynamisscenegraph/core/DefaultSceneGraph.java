package org.dynamisscenegraph.core;

import org.dynamisscenegraph.api.RenderItem;
import org.dynamisscenegraph.api.RenderScene;
import org.dynamisscenegraph.api.SceneGraph;
import org.dynamisscenegraph.api.SceneNode;
import org.dynamisscenegraph.api.SceneNodeId;
import org.dynamisscenegraph.api.extract.BatchedRenderScene;
import org.dynamisscenegraph.api.extract.InstanceBatch;
import org.dynamisscenegraph.api.extract.RenderKey;
import org.dynamisscenegraph.api.value.BoundingSphere;
import org.vectrix.affine.Transformf;
import org.vectrix.core.Matrix4f;
import org.vectrix.core.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 *
 * Does not yet own bounds/culling (Phase 4).
 */
public final class DefaultSceneGraph implements SceneGraph {

    private final Map<SceneNodeId, Node> nodes = new HashMap<>();
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
     * Nodes without bindings are omitted from {@link #extract()}.
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
        updateWorldMatrices();
        Node node = requireNode(nodeId);
        return node.worldBounds == null ? null : BoundingSphere.of(node.worldBounds.center(), node.worldBounds.radius());
    }

    @Override
    public RenderScene extract() {
        updateWorldMatrices();

        List<RenderItem> items = new ArrayList<>();
        for (Node node : this.nodes.values()) {
            if (!node.visible) {
                continue;
            }
            if (node.meshHandle == null && node.materialKey == null) {
                continue;
            }
            items.add(new RenderItem(node.id, new Matrix4f(node.world), node.meshHandle, node.materialKey));
        }
        return new RenderScene(List.copyOf(items));
    }

    public RenderScene extractCulled(Matrix4f viewProj) {
        updateWorldMatrices();

        List<RenderItem> items = new ArrayList<>();
        for (Node node : this.nodes.values()) {
            if (!node.visible) {
                continue;
            }
            if (node.meshHandle == null && node.materialKey == null) {
                continue;
            }
            if (node.worldBounds != null) {
                Vector3f center = node.worldBounds.center();
                if (!viewProj.testSphere(center.x(), center.y(), center.z(), node.worldBounds.radius())) {
                    continue;
                }
            }
            items.add(new RenderItem(node.id, new Matrix4f(node.world), node.meshHandle, node.materialKey));
        }
        return new RenderScene(List.copyOf(items));
    }

    public BatchedRenderScene extractBatched() {
        updateWorldMatrices();

        Map<RenderKey, MutableBatch> grouped = new LinkedHashMap<>();
        for (SceneNodeId nodeId : sortedNodeIds()) {
            Node node = requireNode(nodeId);
            if (!isRenderable(node)) {
                continue;
            }
            addToBatch(grouped, node);
        }
        return new BatchedRenderScene(toBatches(grouped));
    }

    public BatchedRenderScene extractBatchedCulled(Matrix4f viewProj) {
        updateWorldMatrices();

        Map<RenderKey, MutableBatch> grouped = new LinkedHashMap<>();
        for (SceneNodeId nodeId : sortedNodeIds()) {
            Node node = requireNode(nodeId);
            if (!isRenderable(node)) {
                continue;
            }
            if (node.worldBounds != null) {
                Vector3f center = node.worldBounds.center();
                if (!viewProj.testSphere(center.x(), center.y(), center.z(), node.worldBounds.radius())) {
                    continue;
                }
            }
            addToBatch(grouped, node);
        }
        return new BatchedRenderScene(toBatches(grouped));
    }

    public List<SceneNodeId> queryRadius(Vector3f center, float radius) {
        if (radius < 0f) {
            throw new IllegalArgumentException("radius must be >= 0, got: " + radius);
        }

        updateWorldMatrices();
        List<SceneNodeId> hits = new ArrayList<>();
        for (Node node : this.nodes.values()) {
            if (node.worldBounds == null) {
                continue;
            }
            if (intersects(node.worldBounds, center, radius)) {
                hits.add(node.id);
            }
        }
        return List.copyOf(hits);
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

        updateWorldMatrices();
        SceneNodeId best = null;
        float bestT = maxDist;
        for (Node node : this.nodes.values()) {
            if (!node.visible || node.worldBounds == null) {
                continue;
            }
            float t = intersectRaySphere(origin, direction, node.worldBounds);
            if (t >= 0f && t <= bestT) {
                bestT = t;
                best = node.id;
            }
        }
        return Optional.ofNullable(best);
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

    private static boolean intersects(BoundingSphere sphere, Vector3f center, float radius) {
        float r = sphere.radius() + radius;
        return sphere.center().distanceSquared(center) <= r * r;
    }

    private static boolean isRenderable(Node node) {
        if (!node.visible) {
            return false;
        }
        return node.meshHandle != null && node.materialKey != null;
    }

    private static void addToBatch(Map<RenderKey, MutableBatch> grouped, Node node) {
        RenderKey key = RenderKey.of(node.meshHandle, node.materialKey);
        MutableBatch batch = grouped.computeIfAbsent(key, ignored -> new MutableBatch());
        batch.nodeIds.add(node.id);
        batch.worldMatrices.add(new Matrix4f(node.world));
    }

    private static List<InstanceBatch> toBatches(Map<RenderKey, MutableBatch> grouped) {
        List<InstanceBatch> batches = new ArrayList<>();
        for (Map.Entry<RenderKey, MutableBatch> entry : grouped.entrySet()) {
            MutableBatch batch = entry.getValue();
            batches.add(new InstanceBatch(
                    entry.getKey(),
                    List.copyOf(batch.nodeIds),
                    List.copyOf(batch.worldMatrices)
            ));
        }
        return List.copyOf(batches);
    }

    private List<SceneNodeId> sortedNodeIds() {
        List<SceneNodeId> ids = new ArrayList<>(this.nodes.keySet());
        ids.sort(Comparator.comparingLong(SceneNodeId::value));
        return ids;
    }

    private static float intersectRaySphere(Vector3f origin, Vector3f dir, BoundingSphere sphere) {
        Vector3f oc = new Vector3f(origin).sub(sphere.center());
        float b = oc.dot(dir);
        float c = oc.dot(oc) - sphere.radius() * sphere.radius();
        float discriminant = b * b - c;
        if (discriminant < 0f) {
            return -1f;
        }
        float sqrt = (float) Math.sqrt(discriminant);
        float t0 = -b - sqrt;
        float t1 = -b + sqrt;
        if (t0 >= 0f) {
            return t0;
        }
        return t1 >= 0f ? t1 : -1f;
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

    private static final class MutableBatch {
        private final List<SceneNodeId> nodeIds = new ArrayList<>();
        private final List<Matrix4f> worldMatrices = new ArrayList<>();
    }
}
