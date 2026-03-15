package org.dynamisengine.scenegraph.api;

import org.dynamisengine.core.entity.EntityId;

import java.util.Optional;

public record SceneNodeMetadata(
        SceneNodeId id,
        EntityId ownerEntityId
) {
    public Optional<EntityId> owner() {
        return Optional.ofNullable(ownerEntityId);
    }
}
