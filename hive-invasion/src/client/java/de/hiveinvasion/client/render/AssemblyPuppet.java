package de.hiveinvasion.client.render;

import de.hiveinvasion.client.model.ExoSuitModel;
import de.hiveinvasion.exo.ExoSuitEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Zeichnet den Spieler während des Anlegens als animierte Figur:
 * umdrehen (0–15), rückwärts in die Stiefel gehen (15–38), Arme zur Seite in die
 * Handschuhe strecken (40–50), Arme mit dem Anzug senken (85–100).
 */
public final class AssemblyPuppet {
    private AssemblyPuppet() {}

    public static void render(ExoSuitEntity suit, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (suit.getAssembler().isEmpty()) return;
        PlayerEntity p = suit.getWorld().getPlayerByUuid(suit.getAssembler().get());
        if (!(p instanceof AbstractClientPlayerEntity player)) return;
        if (!(MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer)) return;
        PlayerEntityModel<AbstractClientPlayerEntity> m = renderer.getModel();
        float T = suit.getAssembly(tickDelta) * ExoSuitEntity.ASSEMBLE_TICKS;
        if (T >= ExoSuitEntity.ASSEMBLE_TICKS) return;

        // Position und Blickrichtung
        float suitYaw = suit.getYaw();
        float turn = ExoSuitModel.stage(T, 0, 15);
        float yaw = suit.getStartYaw() + MathHelper.wrapDegrees(suitYaw - suit.getStartYaw()) * turn;
        float walk = ExoSuitModel.stage(T, 15, 23);
        float dx = suit.getStartDx() * (1 - walk), dz = suit.getStartDz() * (1 - walk);
        float lift = 3f / 16f * ExoSuitModel.stage(T, 30, 8);    // auf die Sohlen steigen
        boolean walking = T > 15 && T < 38;

        for (var part : new net.minecraft.client.model.ModelPart[]{m.head, m.hat, m.body, m.jacket, m.rightArm, m.leftArm,
                m.rightSleeve, m.leftSleeve, m.rightLeg, m.leftLeg, m.rightPants, m.leftPants}) part.resetTransform();
        m.riding = false; m.sneaking = false; m.child = false;

        float step = walking ? MathHelper.sin(T * 0.7f) * 0.55f : 0;
        m.rightLeg.pitch = -step;     // rückwärts gehen
        m.leftLeg.pitch = step;
        float armsOut = ExoSuitModel.stage(T, 40, 10) * (1 - ExoSuitModel.stage(T, 85, 15));
        m.rightArm.roll = 1.5f * armsOut;
        m.leftArm.roll = -1.5f * armsOut;
        m.rightArm.pitch = step * 0.6f * (1 - armsOut);
        m.leftArm.pitch = -step * 0.6f * (1 - armsOut);
        m.head.pitch = T < 15 ? 0.1f : -0.05f;
        m.hat.copyTransform(m.head);
        m.jacket.copyTransform(m.body);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightPants.copyTransform(m.rightLeg);
        m.leftPants.copyTransform(m.leftLeg);

        matrices.push();
        matrices.translate(dx, lift, dz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - yaw));
        matrices.scale(-1, -1, 1);
        matrices.scale(0.9375f, 0.9375f, 0.9375f);
        matrices.translate(0, -1.501, 0);
        m.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(renderer.getTexture(player))), light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
