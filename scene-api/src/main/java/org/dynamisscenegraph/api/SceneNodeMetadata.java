package org.dynamisscenegraph.api;

import org.dynamis.core.entity.EntityId;

import java.util.Optional;

public record SceneNodeMetadata(
        SceneNodeId id,
        EntityId ownerEntityId
) {
    public Optional<EntityId> owner() {
        return Optional.ofNullable(ownerEntityId);
    }
}
