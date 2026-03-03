package org.dynamisscenegraph.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SceneNodeIdTest {

    @Test
    void rejectsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> SceneNodeId.of(0));
        assertThrows(IllegalArgumentException.class, () -> SceneNodeId.of(-1));
    }

    @Test
    void acceptsPositive() {
        SceneNodeId id = SceneNodeId.of(1);
        assertEquals(1, id.value());
    }
}
