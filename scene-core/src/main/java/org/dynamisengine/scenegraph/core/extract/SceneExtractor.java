package org.dynamisengine.scenegraph.core.extract;

import org.dynamisengine.scenegraph.core.DefaultSceneGraph;

@FunctionalInterface
public interface SceneExtractor<T> {
    T extract(DefaultSceneGraph graph);
}
