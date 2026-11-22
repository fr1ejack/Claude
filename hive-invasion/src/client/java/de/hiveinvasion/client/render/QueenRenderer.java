package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.client.model.QueenModel;
import de.hiveinvasion.hive.QueenEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class QueenRenderer extends MobEntityRenderer<QueenEntity, QueenModel> {
    private static final Identifier TEX = HiveInvasion.id("textures/entity/queen.png");

    public QueenRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new QueenModel(ctx.getPart(QueenModel.LAYER)), 1.6f);
    }

    @Override public Identifier getTexture(QueenEntity e) { return TEX; }

    @Override
    protected void scale(QueenEntity e, net.minecraft.client.util.math.MatrixStack m, float amount) {
        m.scale(1.65f, 1.65f, 1.65f);
    }
}
