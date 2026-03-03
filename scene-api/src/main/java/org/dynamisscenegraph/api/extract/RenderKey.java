package org.dynamisscenegraph.api.extract;

import java.util.Objects;

public record RenderKey(Object meshHandle, Object materialKey) {

    public RenderKey {
        Objects.requireNonNull(meshHandle, "meshHandle");
        Objects.requireNonNull(materialKey, "materialKey");
    }

    public static RenderKey of(Object meshHandle, Object materialKey) {
        return new RenderKey(meshHandle, materialKey);
    }
}
