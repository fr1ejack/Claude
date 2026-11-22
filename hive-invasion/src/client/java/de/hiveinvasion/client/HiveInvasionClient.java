package de.hiveinvasion.client;

import de.hiveinvasion.client.model.ClingerModel;
import de.hiveinvasion.client.model.LarvaModel;
import de.hiveinvasion.client.model.StalkerModel;
import de.hiveinvasion.client.render.AlienRenderer;
import de.hiveinvasion.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class HiveInvasionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(StalkerModel.LAYER, StalkerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ClingerModel.LAYER, ClingerModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(LarvaModel.LAYER, LarvaModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.STALKER, ctx ->
                new AlienRenderer<>(ctx, new StalkerModel<>(ctx.getPart(StalkerModel.LAYER)), 0.6f, "stalker", 1.1f));
        EntityRendererRegistry.register(ModEntities.CLINGER, ctx ->
                new AlienRenderer<>(ctx, new ClingerModel<>(ctx.getPart(ClingerModel.LAYER)), 0.35f, "clinger", 1.0f));
        EntityRendererRegistry.register(ModEntities.LARVA, ctx ->
                new AlienRenderer<>(ctx, new LarvaModel<>(ctx.getPart(LarvaModel.LAYER)), 0.25f, "larva", 1.0f));
    }
}
