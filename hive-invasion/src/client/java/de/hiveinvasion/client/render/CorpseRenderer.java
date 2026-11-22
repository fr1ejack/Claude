package de.hiveinvasion.client.render;

import com.mojang.authlib.GameProfile;
import de.hiveinvasion.entity.CorpseEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Zeichnet die Leiche: ein unbewegliches Abbild des Toten, auf der Seite liegend,
 * mit leicht verdrehtem Kopf.
 */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    public CorpseRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    private static LivingEntity body(CorpseEntity corpse) {
        if (corpse.renderCache instanceof LivingEntity e) return e;
        ClientWorld world = (ClientWorld) corpse.getWorld();
        LivingEntity e = null;
        if (corpse.isPlayerCorpse()) {
            e = new OtherClientPlayerEntity(world, new GameProfile(corpse.getPlayerId().get(), corpse.getPlayerName()));
        } else if (!corpse.getBody().isEmpty()) {
            Entity made = EntityType.getEntityFromNbt(corpse.getBody(), world).orElse(null);
            if (made instanceof LivingEntity living) e = living;
        }
        if (e == null) return null;
        e.setCustomNameVisible(false);
        e.deathTime = 0;
        e.hurtTime = 0;
        e.setHealth(1);
        float headTwist = (corpse.getUuid().hashCode() % 70) - 35;
        e.setYaw(0); e.prevYaw = 0; e.bodyYaw = 0; e.prevBodyYaw = 0;
        e.headYaw = headTwist; e.prevHeadYaw = headTwist;
        e.setPitch(0); e.prevPitch = 0;
        corpse.renderCache = e;
        return e;
    }

    @Override
    public void render(CorpseEntity corpse, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        if (corpse.getCorpseAge() < CorpseEntity.FADE_IN) return;   // erst die normale Sterbe-Animation zeigen
        LivingEntity body = body(corpse);
        if (body == null) return;
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - corpse.getYaw()));
        float h = body.getHeight(), w = body.getWidth();
        // Auf die Seite legen (wie die Umfall-Animation) und mittig ausrichten.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f));
        matrices.translate(w * 0.42f, -h * 0.5f, 0);
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(body);
        renderer.render(body, 0, 0, matrices, consumers, light);
        matrices.pop();
        super.render(corpse, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(CorpseEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
