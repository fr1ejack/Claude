package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.client.model.ExoSuitModel;
import de.hiveinvasion.exo.ExoSuitEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class ExoSuitRenderer extends MobEntityRenderer<ExoSuitEntity, ExoSuitModel> {
    /** Der Anzug ist körpernah modelliert (Maßstab 1:1). */
    public static final float SCALE = 1.0f;
    private static final Identifier TEXTURE = HiveInvasion.id("textures/entity/exosuit.png");
    private static final Identifier GLOW = HiveInvasion.id("textures/entity/exosuit_glow.png");
    private static final Identifier SHIELD = HiveInvasion.id("textures/entity/exo_shield.png");

    public ExoSuitRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new ExoSuitModel(ctx.getPart(ExoSuitModel.LAYER)), 0.8f);
        addFeature(new EyesFeatureRenderer<>(this) {
            @Override
            public RenderLayer getEyesTexture() {
                return RenderLayer.getEyes(GLOW);
            }
        });
    }

    @Override
    public Identifier getTexture(ExoSuitEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(ExoSuitEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public void render(ExoSuitEntity suit, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean ownSuitFirstPerson = suit.hasPassenger(mc.player) && mc.options.getPerspective().isFirstPerson();
        if (!ownSuitFirstPerson) super.render(suit, yaw, tickDelta, matrices, consumers, light);
        AssemblyPuppet.render(suit, tickDelta, matrices, consumers, light);
        if (suit.isShieldActive()) renderShield(suit, tickDelta, matrices, consumers);
    }

    /** Leuchtende, flackernde Energiewand vor dem Anzug. */
    private void renderShield(ExoSuitEntity suit, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers) {
        matrices.push();
        float bodyYaw = suit.prevBodyYaw + (suit.bodyYaw - suit.prevBodyYaw) * tickDelta;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0, 0, 1.15);
        float pulse = 0.75f + 0.25f * (float) Math.sin((suit.age + tickDelta) * 0.6f);
        int a = (int) (170 * pulse);
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(SHIELD));
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float w = 1.2f, h0 = 0.1f, h1 = 2.7f;
        float scroll = ((suit.age + tickDelta) * 0.02f) % 1f;
        // Vorder- und Rückseite
        quad(vc, e, m, -w, h0, w, h1, scroll, a, 1);
        quad(vc, e, m, w, h0, -w, h1, scroll, a, -1);
        matrices.pop();
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, Matrix4f m, float x0, float y0, float x1, float y1,
                             float v, int a, float nz) {
        int light = 0xF000F0;
        vc.vertex(m, x0, y0, 0).color(120, 220, 255, a).texture(0, 1 + v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 0, nz);
        vc.vertex(m, x1, y0, 0).color(120, 220, 255, a).texture(1, 1 + v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 0, nz);
        vc.vertex(m, x1, y1, 0).color(120, 220, 255, a).texture(1, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 0, nz);
        vc.vertex(m, x0, y1, 0).color(120, 220, 255, a).texture(0, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 0, nz);
    }
}
