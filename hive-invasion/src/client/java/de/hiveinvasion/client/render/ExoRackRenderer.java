package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.client.model.ExoRackModel;
import de.hiveinvasion.exo.ExoRackEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ExoRackRenderer extends EntityRenderer<ExoRackEntity> {
    private static final Identifier TEXTURE = HiveInvasion.id("textures/entity/exorack.png");
    private static final Identifier GLOW = HiveInvasion.id("textures/entity/exorack_glow.png");
    private final ExoRackModel model;

    public ExoRackRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new ExoRackModel(ctx.getPart(ExoRackModel.LAYER));
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(ExoRackEntity rack, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - rack.getYaw()));
        matrices.scale(-1, -1, 1);
        matrices.translate(0, -1.501, 0);
        model.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
        model.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV);
        matrices.pop();
        super.render(rack, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(ExoRackEntity entity) { return TEXTURE; }
}
