package org.dynamisengine.scenegraph.api.extract;

import org.dynamisengine.scenegraph.api.SceneNodeId;
import org.junit.jupiter.api.Test;
import org.dynamisengine.vectrix.core.Matrix4f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InstanceBatchTest {

    @Test
    void rejectsMismatchedNodeAndMatrixSizes() {
        assertThrows(IllegalArgumentException.class, () -> new InstanceBatch(
                RenderKey.of("mesh", "mat"),
                List.of(SceneNodeId.of(1), SceneNodeId.of(2)),
                List.of(new Matrix4f())
        ));
    }

    @Test
    void reportsInstanceCount() {
        InstanceBatch batch = new InstanceBatch(
                RenderKey.of("mesh", "mat"),
                List.of(SceneNodeId.of(1), SceneNodeId.of(2)),
                List.of(new Matrix4f(), new Matrix4f())
        );
        assertEquals(2, batch.instanceCount());
    }

    @Test
    void renderKeyRejectsNulls() {
        assertThrows(NullPointerException.class, () -> RenderKey.of(null, "mat"));
        assertThrows(NullPointerException.class, () -> RenderKey.of("mesh", null));
    }
}
