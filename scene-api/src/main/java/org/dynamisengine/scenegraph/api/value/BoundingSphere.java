package org.dynamisengine.scenegraph.api.value;

import org.dynamisengine.vectrix.core.Vector3f;

import java.util.Objects;

public record BoundingSphere(Vector3f center, float radius) {

    public BoundingSphere {
        Objects.requireNonNull(center, "center");
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be >= 0, got: " + radius);
        }
        center = new Vector3f(center);
    }

    public static BoundingSphere of(Vector3f center, float radius) {
        return new BoundingSphere(center, radius);
    }
}
