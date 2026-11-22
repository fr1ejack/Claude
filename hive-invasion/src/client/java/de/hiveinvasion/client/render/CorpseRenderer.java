package de.hiveinvasion.client.render;

import com.mojang.authlib.GameProfile;
import de.hiveinvasion.client.ragdoll.Ragdoll;
import de.hiveinvasion.client.ragdoll.Rig;
import de.hiveinvasion.entity.CorpseEntity;
import de.hiveinvasion.fx.BloodFx;
import de.hiveinvasion.gore.Limb;
import de.hiveinvasion.registry.ModParticles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Zeichnet Leichen als Ragdoll: der Körper kippt physikalisch um und rollt,
 * Kopf, Arme und Beine hängen als einzelne, an Gelenken verbundene Glieder.
 * Abgetrennte Glieder fehlen und die Stümpfe bluten eine Weile.
 */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    public CorpseRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6f;
    }

    /** Alles, was der Client zu einer Leiche zwischenspeichert. */
    static final class Visual {
        LivingEntity body;
        Rig rig;
        Ragdoll ragdoll;
        int lastAge = -1;
        Vec3d lastVel = Vec3d.ZERO;
    }

    private static Visual visual(CorpseEntity corpse) {
        if (corpse.renderCache instanceof Visual v) return v;
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

        Visual v = new Visual();
        v.body = e;
        EntityRenderer<? super LivingEntity> r = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e);
        v.rig = Rig.create(r, e);
        if (v.rig == null) v.rig = Rig.rigid(e.getHeight(), e.getWidth());
        Vec3d vel = corpse.getVelocity();
        float fallYaw = vel.horizontalLengthSquared() > 1e-4
                ? (float) Math.toDegrees(Math.atan2(vel.x, vel.z))
                : corpse.getYaw() + 180f + (corpse.getUuid().hashCode() % 90);
        v.ragdoll = new Ragdoll(v.rig, corpse.getUuid().getLeastSignificantBits(), fallYaw);
        if (corpse.getCorpseAge() > 40) v.ragdoll.settle();
        v.lastAge = corpse.age;
        corpse.renderCache = v;
        return v;
    }

    private static void advance(CorpseEntity corpse, Visual v) {
        int steps = Math.min(10, corpse.age - v.lastAge);
        for (int i = 0; i < steps; i++) {
            Vec3d vel = corpse.getVelocity();
            Vec3d dv = vel.subtract(v.lastVel);
            if (dv.lengthSquared() > 0.0025) v.ragdoll.impulse((float) dv.x, (float) Math.max(0, dv.y), (float) dv.z);
            v.lastVel = vel;
            v.ragdoll.tick();
        }
        v.lastAge = corpse.age;
    }

    @Override
    public void render(CorpseEntity corpse, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        Visual v = visual(corpse);
        if (v == null) return;
        advance(corpse, v);
        Ragdoll.Pose pose = v.ragdoll.pose(tickDelta);
        int severed = corpse.getSevered();

        matrices.push();
        if (v.rig.isRigid) {
            matrices.multiply(pose.torso);
            // Ganzer Körper als Block – aber physikalisch umgekippt
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(v.body);
            renderer.render(v.body, 0, 0, matrices, consumers, light);
        } else {
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(v.rig.texture));
            int overlay = OverlayTexture.DEFAULT_UV;
            // Rumpf
            renderSegment(v.rig.torso, pose.pelvis, pose.torso, new Vector3f(v.rig.torso.joint), matrices, vc, light, overlay, v.rig.scale);
            // Glieder
            for (int i = 0; i < v.rig.limbs.size(); i++) {
                Rig.Segment s = v.rig.limbs.get(i);
                if (s.limb != null && s.limb.in(severed)) {
                    bleedStump(corpse, pose.joint[i]);
                    continue;
                }
                matrices.push();
                matrices.translate(pose.joint[i].x, pose.joint[i].y, pose.joint[i].z);
                matrices.multiply(pose.limbRot[i]);
                drawParts(s, matrices, vc, light, overlay, v.rig.scale);
                matrices.pop();
            }
        }
        matrices.pop();
        super.render(corpse, yaw, tickDelta, matrices, consumers, light);
    }

    private static void renderSegment(Rig.Segment s, Vector3f pelvis, Quaternionf rot, Vector3f anchorLocal,
                                      MatrixStack matrices, VertexConsumer vc, int light, int overlay, float scale) {
        matrices.push();
        matrices.translate(pelvis.x, pelvis.y, pelvis.z);
        matrices.multiply(rot);
        matrices.translate(anchorLocal.x, anchorLocal.y, anchorLocal.z);
        drawParts(s, matrices, vc, light, overlay, scale);
        matrices.pop();
    }

    /** Zeichnet die Modellteile eines Glieds mit Drehpunkt im Ursprung. */
    private static void drawParts(Rig.Segment s, MatrixStack matrices, VertexConsumer vc, int light, int overlay, float scale) {
        matrices.scale(-scale, -scale, scale);
        List<Boolean> childVis = new ArrayList<>();
        for (ModelPart c : s.hiddenChildren) { childVis.add(c.visible); c.visible = false; }
        for (ModelPart p : s.parts) {
            ModelTransform saved = p.getTransform();
            boolean vis = p.visible;
            p.setPivot(0, 0, 0);
            p.setAngles(0, 0, 0);
            p.visible = true;
            p.render(matrices, vc, light, overlay);
            p.setTransform(saved);
            p.visible = vis;
        }
        for (int i = 0; i < s.hiddenChildren.size(); i++) s.hiddenChildren.get(i).visible = childVis.get(i);
    }

    private static void bleedStump(CorpseEntity corpse, Vector3f joint) {
        if (corpse.getCorpseAge() > 20 * 20 || corpse.getWorld().random.nextInt(6) != 0) return;
        corpse.getWorld().addParticle(BloodFx.Kind.ACID == corpse.bloodKind() ? ModParticles.ACID_BLOOD : ModParticles.BLOOD,
                corpse.getX() + joint.x, corpse.getY() + joint.y, corpse.getZ() + joint.z,
                (corpse.getRandom().nextFloat() - 0.5f) * 0.08f, 0.02f, (corpse.getRandom().nextFloat() - 0.5f) * 0.08f);
    }

    @Override
    public Identifier getTexture(CorpseEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
