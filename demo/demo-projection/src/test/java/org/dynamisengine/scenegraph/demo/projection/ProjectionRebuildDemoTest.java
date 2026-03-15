package org.dynamisengine.scenegraph.demo.projection;

import org.dynamisengine.ecs.api.world.World;
import org.dynamisengine.ecs.core.DefaultWorld;
import org.dynamissession.api.model.EcsSnapshot;
import org.dynamissession.api.model.SaveGame;
import org.dynamissession.api.model.SaveMetadata;
import org.dynamissession.runtime.DefaultSessionManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectionRebuildDemoTest {

    @Test
    void saveLoadAndRebuildShouldProduceSingleBatchWithExpectedInstances() throws Exception {
        DefaultSessionManager manager = new DefaultSessionManager();
        var registry = ProjectionRebuildDemo.createRegistry();

        DefaultWorld world = (DefaultWorld) manager.newGame();
        ProjectionRebuildDemo.populateWorld(world, 3);

        SaveGame save = new SaveGame(
                new SaveMetadata(1, "1.0.0-SNAPSHOT", System.currentTimeMillis(), 11L, "test-slot"),
                new EcsSnapshot(List.of()));

        Path slot = Files.createTempFile("projection-rebuild-test-", ".dses");
        manager.save(slot, world, save, registry);

        World loaded = manager.load(slot, registry);
        var scene = ProjectionRebuildDemo.rebuildProjection(loaded);

        assertEquals(1, scene.batches().size());
        assertEquals(3, ProjectionRebuildDemo.totalInstances(scene));
    }
}
