package org.dynamisscenegraph.core.extract;

import org.dynamisscenegraph.core.DefaultSceneGraph;

@FunctionalInterface
public interface SceneExtractor<T> {
    T extract(DefaultSceneGraph graph);
}
