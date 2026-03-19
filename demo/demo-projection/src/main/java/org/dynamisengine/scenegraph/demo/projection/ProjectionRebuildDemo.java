package org.dynamisengine.scenegraph.demo.projection;

import org.dynamisengine.core.entity.EntityId;
import org.dynamisengine.ecs.api.world.World;
import org.dynamisengine.ecs.core.DefaultWorld;
import org.dynamisengine.scenegraph.api.extract.BatchedRenderScene;
import org.dynamisengine.scenegraph.core.DefaultSceneGraph;
import org.dynamisengine.scenegraph.demo.projection.codecs.BoundsSphereCodec;
import org.dynamisengine.scenegraph.demo.projection.codecs.RenderableCodec;
import org.dynamisengine.scenegraph.demo.projection.codecs.TranslationCodec;
import org.dynamisengine.scenegraph.demo.projection.components.BoundsSphereComponent;
import org.dynamisengine.scenegraph.demo.projection.components.RenderableComponent;
import org.dynamisengine.scenegraph.demo.projection.components.TranslationComponent;
import org.dynamissession.api.model.EcsSnapshot;
import org.dynamissession.api.model.SaveGame;
import org.dynamissession.api.model.SaveMetadata;
import org.dynamissession.core.codec.DefaultCodecRegistry;
import org.dynamissession.runtime.DefaultSessionManager;
import org.dynamisengine.vectrix.affine.Transformf;
import org.dynamisengine.vectrix.core.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ProjectionRebuildDemo {

    private ProjectionRebuildDemo() {
    }

    public static void main(String[] args) throws Exception {
        int entityCount = 25;

        DefaultSessionManager manager = new DefaultSessionManager();
        DefaultCodecRegistry registry = createRegistry();

        DefaultWorld world = (DefaultWorld) manager.newGame();
        populateWorld(world, entityCount);

        Path slot = Files.createTempFile("scenegraph-projection-demo-", ".dses");
        SaveGame save = new SaveGame(
                new SaveMetadata(1, "1.0.0-SNAPSHOT", System.currentTimeMillis(), 100L, "projection-demo"),
                new EcsSnapshot(List.of()));

        Set<EntityId> beforeIds = new LinkedHashSet<>(world.entities());
        manager.save(slot, world, save, registry);

        World loaded = manager.load(slot, registry);
        Set<EntityId> afterIds = new LinkedHashSet<>(loaded.entities());
        if (!beforeIds.equals(afterIds)) {
            throw new IllegalStateException("Loaded EntityIds differ from saved EntityIds");
        }

        BatchedRenderScene batched = rebuildProjection(loaded);
        int totalInstances = totalInstances(batched);

        System.out.println("Projection rebuild demo");
        System.out.println("slot=" + slot);
        System.out.println("batches=" + batched.batches().size());
        for (int i = 0; i < batched.batches().size(); i++) {
            System.out.println("batch[" + i + "] instances=" + batched.batches().get(i).instanceCount());
        }
        System.out.println("totalInstances=" + totalInstances);

        if (batched.batches().size() != 1) {
            throw new IllegalStateException("Expected 1 batch, got " + batched.batches().size());
        }
        if (totalInstances != entityCount) {
            throw new IllegalStateException("Expected " + entityCount + " instances, got " + totalInstances);
        }
    }

    static DefaultCodecRegistry createRegistry() {
        DefaultCodecRegistry registry = new DefaultCodecRegistry();
        registry.register(new TranslationCodec());
        registry.register(new BoundsSphereCodec());
        registry.register(new RenderableCodec());
        return registry;
    }

    static void populateWorld(DefaultWorld world, int entityCount) {
        for (int i = 0; i < entityCount; i++) {
            EntityId entity = world.createEntity();
            int x = i % 5;
            int z = i / 5;
            world.add(entity, DemoKeys.TRANSLATION, new TranslationComponent(x, 0f, z));
            world.add(entity, DemoKeys.BOUNDS, new BoundsSphereComponent(0f, 0f, 0f, 0.5f));
            world.add(entity, DemoKeys.RENDERABLE, new RenderableComponent(1, "mat.default"));
        }
    }

    static BatchedRenderScene rebuildProjection(World world) {
        DefaultSceneGraph graph = new DefaultSceneGraph();

        for (EntityId entity : world.entities()) {
            var translation = world.get(entity, DemoKeys.TRANSLATION);
            var bounds = world.get(entity, DemoKeys.BOUNDS);
            var renderable = world.get(entity, DemoKeys.RENDERABLE);
            if (translation.isEmpty() || bounds.isEmpty() || renderable.isEmpty()) {
                continue;
            }

            TranslationComponent t = translation.get();
            BoundsSphereComponent b = bounds.get();
            RenderableComponent r = renderable.get();

            var node = graph.createNode();
            Transformf transform = new Transformf();
            transform.translation.set(t.x(), t.y(), t.z());
            graph.setLocalTransform(node, transform);
            graph.setLocalBoundsSphere(node, new Vector3f(b.cx(), b.cy(), b.cz()), b.radius());
            graph.bindRenderable(node, Integer.valueOf(r.meshHandle()), r.materialKey());
        }

        return graph.extractBatched();
    }

    static int totalInstances(BatchedRenderScene scene) {
        return scene.batches().stream().mapToInt(batch -> batch.instanceCount()).sum();
    }
}
