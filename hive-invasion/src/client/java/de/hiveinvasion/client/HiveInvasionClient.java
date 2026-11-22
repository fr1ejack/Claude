package de.hiveinvasion.client;

import de.hiveinvasion.client.model.ClingerModel;
import de.hiveinvasion.client.model.ExoSuitModel;
import de.hiveinvasion.client.model.LarvaModel;
import de.hiveinvasion.client.model.StalkerModel;
import de.hiveinvasion.client.particle.BloodParticle;
import de.hiveinvasion.client.render.AlienRenderer;
import de.hiveinvasion.client.render.CorpseRenderer;
import de.hiveinvasion.client.render.ExoSuitRenderer;
import de.hiveinvasion.registry.ModBlocks;
import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class HiveInvasionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(StalkerModel.LAYER, StalkerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ClingerModel.LAYER, ClingerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(LarvaModel.LAYER, LarvaModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ExoSuitModel.LAYER, ExoSuitModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.STALKER, ctx ->
                new AlienRenderer<>(ctx, new StalkerModel<>(ctx.getPart(StalkerModel.LAYER)), 0.6f, "stalker", 1.1f));
        EntityRendererRegistry.register(ModEntities.CLINGER, ctx ->
                new AlienRenderer<>(ctx, new ClingerModel<>(ctx.getPart(ClingerModel.LAYER)), 0.35f, "clinger", 1.0f));
        EntityRendererRegistry.register(ModEntities.LARVA, ctx ->
                new AlienRenderer<>(ctx, new LarvaModel<>(ctx.getPart(LarvaModel.LAYER)), 0.25f, "larva", 1.0f));
        EntityRendererRegistry.register(ModEntities.CORPSE, CorpseRenderer::new);
        EntityRendererRegistry.register(ModEntities.EXO_SUIT, ExoSuitRenderer::new);
        EntityRendererRegistry.register(ModEntities.PLASMA_BOLT, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.2f, true));

        ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
        particles.register(ModParticles.BLOOD, s -> new BloodParticle.Factory(s, 0.55f, 0.02f, 0.02f, false, 0.9f));
        particles.register(ModParticles.ACID_BLOOD, s -> new BloodParticle.Factory(s, 0.6f, 0.95f, 0.15f, true, 0.9f));
        particles.register(ModParticles.PLASMA, s -> new BloodParticle.Factory(s, 0.4f, 0.85f, 1.0f, true, 0.0f));

        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.BLOOD_PUDDLE, ModBlocks.ACID_PUDDLE);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.BROOD_POD);

        ExoClient.register();
    }
}
