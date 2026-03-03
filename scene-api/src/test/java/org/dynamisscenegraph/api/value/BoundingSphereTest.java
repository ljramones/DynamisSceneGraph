package org.dynamisscenegraph.api.value;

import org.junit.jupiter.api.Test;
import org.vectrix.core.Vector3f;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BoundingSphereTest {

    @Test
    void rejectsNegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> BoundingSphere.of(new Vector3f(), -1f));
    }

    @Test
    void rejectsNullCenter() {
        assertThrows(NullPointerException.class, () -> BoundingSphere.of(null, 1f));
    }

    @Test
    void acceptsValidSphere() {
        assertDoesNotThrow(() -> BoundingSphere.of(new Vector3f(1f, 2f, 3f), 5f));
    }
}
