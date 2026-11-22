package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public class AlienRenderer<T extends MobEntity, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
    private final Identifier texture;
    private final float scale;

    public AlienRenderer(EntityRendererFactory.Context ctx, M model, float shadow, String name, float scale) {
        super(ctx, model, shadow);
        this.texture = HiveInvasion.id("textures/entity/" + name + ".png");
        this.scale = scale;
    }

    public float getScale() {
        return scale;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
        if (entity instanceof de.hiveinvasion.entity.StalkerEntity s && s.isCarrying()) {
            matrices.push();
            float body = net.minecraft.util.math.MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-body));
            matrices.translate(-0.3, 0.95, 0.75);
            matrices.scale(0.6f, 0.6f, 0.6f);
            net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                    de.hiveinvasion.registry.ModBlocks.BROOD_POD.getDefaultState(), matrices, consumers, light,
                    net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }
}
