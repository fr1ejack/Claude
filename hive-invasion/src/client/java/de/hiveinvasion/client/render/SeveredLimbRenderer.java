package de.hiveinvasion.client.render;

import com.mojang.authlib.GameProfile;
import de.hiveinvasion.client.ragdoll.Rig;
import de.hiveinvasion.fx.BloodFx;
import de.hiveinvasion.gore.SeveredLimbEntity;
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
import net.minecraft.util.math.random.Random;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Zeichnet ein abgetrenntes Körperteil, das im Flug trudelt und dann flach liegen bleibt. */
public class SeveredLimbRenderer extends EntityRenderer<SeveredLimbEntity> {
    public SeveredLimbRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.2f;
    }

    static final class Visual {
        Rig rig;
        Rig.Segment segment;
        final Quaternionf rot = new Quaternionf(), prevRot = new Quaternionf();
        final Vector3f spin = new Vector3f();
        int lastAge;
    }

    private static Visual visual(SeveredLimbEntity e) {
        if (e.renderCache instanceof Visual v) return v;
        ClientWorld world = (ClientWorld) e.getWorld();
        LivingEntity body = null;
        if (e.getPlayerId().isPresent()) {
            body = new OtherClientPlayerEntity(world, new GameProfile(e.getPlayerId().get(), e.getPlayerName()));
        } else if (!e.getBody().isEmpty()) {
            Entity made = EntityType.getEntityFromNbt(e.getBody(), world).orElse(null);
            if (made instanceof LivingEntity l) body = l;
        }
        if (body == null) return null;
        Rig rig = Rig.create(MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(body), body);
        if (rig == null || rig.isRigid) return null;
        Visual v = new Visual();
        v.rig = rig;
        for (Rig.Segment s : rig.limbs) if (s.limb == e.getLimb()) v.segment = s;
        if (v.segment == null) return null;
        Random r = Random.create(e.getUuid().getMostSignificantBits());
        v.rot.rotateY(e.getYaw() * 0.0174533f);
        v.spin.set((r.nextFloat() - 0.5f) * 0.9f, (r.nextFloat() - 0.5f) * 0.6f, (r.nextFloat() - 0.5f) * 0.9f);
        v.prevRot.set(v.rot);
        v.lastAge = e.age;
        if (e.getLife() > 60) settle(e, v, 100);
        e.renderCache = v;
        return v;
    }

    private static void settle(SeveredLimbEntity e, Visual v, int steps) {
        for (int i = 0; i < steps; i++) step(e, v, true);
        v.prevRot.set(v.rot);
    }

    private static void step(SeveredLimbEntity e, Visual v, boolean onGround) {
        v.prevRot.set(v.rot);
        if (onGround) {
            v.spin.mul(0.6f);
            // flach hinlegen: Längsachse waagerecht, Drehung um die Hochachse behalten
            Vector3f axis = new Vector3f(v.segment.rest).rotate(v.rot);
            Vector3f flat = new Vector3f(axis.x, 0, axis.z);
            if (flat.lengthSquared() < 1e-4) flat.set(1, 0, 0);
            flat.normalize();
            Quaternionf toFlat = new Quaternionf().rotationTo(axis, flat);
            v.rot.premul(new Quaternionf().slerp(toFlat, 0.25f));
        }
        if (v.spin.lengthSquared() > 1e-6) {
            v.rot.premul(new Quaternionf().rotateXYZ(v.spin.x, v.spin.y, v.spin.z));
        }
    }

    @Override
    public void render(SeveredLimbEntity e, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        Visual v = visual(e);
        if (v == null) return;
        int steps = Math.min(10, e.age - v.lastAge);
        for (int i = 0; i < steps; i++) step(e, v, e.isOnGround());
        v.lastAge = e.age;

        if (e.getLife() < 20 * 15 && e.getWorld().random.nextInt(4) == 0) {
            e.getWorld().addParticle(e.bloodKind() == BloodFx.Kind.ACID ? ModParticles.ACID_BLOOD : ModParticles.BLOOD,
                    e.getX(), e.getY() + 0.15, e.getZ(), 0, 0.01, 0);
        }

        Quaternionf q = new Quaternionf(v.prevRot).slerp(v.rot, tickDelta);
        matrices.push();
        matrices.translate(0, 0.12, 0);
        matrices.multiply(q);
        // Mittelpunkt des Glieds liegt auf der Position des Objekts
        Vector3f half = new Vector3f(v.segment.rest).mul(-v.segment.length * 0.5f);
        matrices.translate(half.x, half.y, half.z);
        float s = v.rig.scale;
        matrices.scale(-s, -s, s);
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(v.rig.texture));
        for (ModelPart p : v.segment.parts) {
            ModelTransform saved = p.getTransform();
            boolean vis = p.visible;
            p.setPivot(0, 0, 0); p.setAngles(0, 0, 0); p.visible = true;
            p.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
            p.setTransform(saved); p.visible = vis;
        }
        matrices.pop();
        super.render(e, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(SeveredLimbEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
