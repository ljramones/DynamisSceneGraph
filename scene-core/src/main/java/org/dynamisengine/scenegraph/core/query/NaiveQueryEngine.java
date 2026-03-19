package org.dynamisengine.scenegraph.core.query;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.dynamisengine.scenegraph.api.value.BoundingSphere;
import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.dynamisengine.vectrix.core.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NaiveQueryEngine implements SceneQueryEngine {

    @Override
    public List<SceneNodeId> queryRadius(DefaultSceneGraph graph, Vector3f center, float radius) {
        graph.ensureWorldUpdated();

        List<SceneNodeId> hits = new ArrayList<>();
        for (DefaultSceneGraph.NodeView view : graph.viewsInStorageOrder()) {
            BoundingSphere bounds = view.worldBounds();
            if (bounds == null) {
                continue;
            }
            if (intersects(bounds, center, radius)) {
                hits.add(view.id());
            }
        }
        return List.copyOf(hits);
    }

    @Override
    public Optional<RaycastHit> raycastCoarse(DefaultSceneGraph graph, Vector3f origin, Vector3f dir, float maxDist) {
        graph.ensureWorldUpdated();

        RaycastHit best = null;
        float bestT = maxDist;
        for (DefaultSceneGraph.NodeView view : graph.viewsInStorageOrder()) {
            if (!view.visible()) {
                continue;
            }
            BoundingSphere bounds = view.worldBounds();
            if (bounds == null) {
                continue;
            }
            float t = intersectRaySphere(origin, dir, bounds);
            if (t >= 0f && t <= bestT) {
                bestT = t;
                best = new RaycastHit(view.id(), t);
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean intersects(BoundingSphere sphere, Vector3f center, float radius) {
        float r = sphere.radius() + radius;
        return sphere.center().distanceSquared(center) <= r * r;
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
}
