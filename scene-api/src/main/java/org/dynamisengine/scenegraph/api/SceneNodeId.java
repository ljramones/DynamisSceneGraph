package org.dynamisengine.scenegraph.api;

public record SceneNodeId(long value) {

    public SceneNodeId {
        if (value <= 0) {
            throw new IllegalArgumentException("SceneNodeId must be positive");
        }
    }

    public static SceneNodeId of(long value) {
        return new SceneNodeId(value);
    }

    @Override
    public String toString() {
        return "SceneNodeId[" + value + "]";
    }
}
