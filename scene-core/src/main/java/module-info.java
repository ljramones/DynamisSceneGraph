module org.dynamisengine.scenegraph.core {
    requires transitive org.dynamisengine.scenegraph.api;
    requires org.dynamisengine.core;
    requires org.vectrix;

    exports org.dynamisengine.scenegraph.core;
    exports org.dynamisengine.scenegraph.core.cull;
    exports org.dynamisengine.scenegraph.core.extract;
    exports org.dynamisengine.scenegraph.core.query;
}
