package de.hiveinvasion.client.ragdoll;

import de.hiveinvasion.client.model.StalkerModel;
import de.hiveinvasion.client.render.AlienRenderer;
import de.hiveinvasion.gore.Limb;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Beschreibt, wie ein Körper in Glieder zerlegt wird: welche Modellteile zum Rumpf,
 * Kopf, zu Armen und Beinen gehören, wo die Gelenke sitzen und wie lang die Glieder sind.
 * Koordinaten in Blöcken, Ursprung = Becken, y nach oben.
 */
public final class Rig {
    public static final class Segment {
        public final Limb limb;              // null = Rumpf
        public final List<ModelPart> parts = new ArrayList<>();
        public final List<ModelPart> hiddenChildren = new ArrayList<>();
        public final Vector3f joint;         // Gelenk im Rumpf-Koordinatensystem
        public final Vector3f rest;          // Ruherichtung (Einheitsvektor)
        public final float length;

        Segment(Limb limb, Vector3f joint, Vector3f rest, float length) {
            this.limb = limb; this.joint = joint; this.rest = rest; this.length = length;
        }
    }

    public final Identifier texture;
    public final EntityModel<?> model;
    public final float scale;
    /** Beinlänge: so hoch steht das Becken über dem Boden. */
    public final float legLength;
    /** Halbe Körperbreite / -tiefe – Abstand zum Boden im Liegen. */
    public final float halfWidth, halfDepth;
    public Segment torso;
    public boolean isRigid;
    public final List<Segment> limbs = new ArrayList<>();

    private Rig(Identifier texture, EntityModel<?> model, float scale, float legLength, float halfWidth, float halfDepth) {
        this.texture = texture; this.model = model; this.scale = scale;
        this.legLength = legLength; this.halfWidth = halfWidth; this.halfDepth = halfDepth;
    }

    /** Modellkoordinate (Pixel, y nach unten) relativ zum Becken → Rumpfkoordinate (Blöcke, y nach oben). */
    private static Vector3f local(float px, float py, float pz, float ox, float oy, float oz, float s) {
        return new Vector3f(-(px - ox) / 16f * s, -(py - oy) / 16f * s, (pz - oz) / 16f * s);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Rig create(EntityRenderer<?> renderer, LivingEntity body) {
        if (!(renderer instanceof LivingEntityRenderer living)) return null;
        EntityModel model = living.getModel();
        Identifier tex = ((EntityRenderer) renderer).getTexture(body);
        if (model instanceof BipedEntityModel<?> biped) return biped(biped, tex);
        if (model instanceof StalkerModel<?> stalker && renderer instanceof AlienRenderer<?, ?> ar) return stalker(stalker, tex, ar.getScale());
        return null;
    }

    private static Rig biped(BipedEntityModel<?> m, Identifier tex) {
        float s = 1f;
        Rig r = new Rig(tex, m, s, 0.75f, 0.25f, 0.13f);
        final float ox = 0, oy = 12, oz = 0;   // Becken
        Segment torso = new Segment(null, local(0, 0, 0, ox, oy, oz, s), new Vector3f(0, 1, 0), 0.75f);
        torso.parts.add(m.body);
        Segment head = new Segment(Limb.HEAD, pivot(m.head, ox, oy, oz, s), new Vector3f(0, 1, 0), 0.5f);
        head.parts.add(m.head); head.parts.add(m.hat);
        Segment ra = new Segment(Limb.RIGHT_ARM, pivot(m.rightArm, ox, oy, oz, s), new Vector3f(0, -1, 0), 0.7f);
        ra.parts.add(m.rightArm);
        Segment la = new Segment(Limb.LEFT_ARM, pivot(m.leftArm, ox, oy, oz, s), new Vector3f(0, -1, 0), 0.7f);
        la.parts.add(m.leftArm);
        Segment rl = new Segment(Limb.RIGHT_LEG, pivot(m.rightLeg, ox, oy, oz, s), new Vector3f(0, -1, 0), 0.75f);
        rl.parts.add(m.rightLeg);
        Segment ll = new Segment(Limb.LEFT_LEG, pivot(m.leftLeg, ox, oy, oz, s), new Vector3f(0, -1, 0), 0.75f);
        ll.parts.add(m.leftLeg);
        if (m instanceof PlayerEntityModel<?> p) {
            torso.parts.add(p.jacket);
            ra.parts.add(p.rightSleeve); la.parts.add(p.leftSleeve);
            rl.parts.add(p.rightPants); ll.parts.add(p.leftPants);
        }
        r.torso = torso;
        r.limbs.add(head); r.limbs.add(ra); r.limbs.add(la); r.limbs.add(rl); r.limbs.add(ll);
        return r;
    }

    /** Für Körper ohne Glieder-Aufteilung (Tiere usw.): kippt als Ganzes um. */
    public static Rig rigid(float height, float width) {
        Rig r = new Rig(null, null, 1f, height * 0.5f, width * 0.5f, width * 0.5f);
        r.isRigid = true;
        return r;
    }

    private static Vector3f pivot(ModelPart p, float ox, float oy, float oz, float s) {
        ModelTransform t = p.getDefaultTransform();
        return local(t.pivotX, t.pivotY, t.pivotZ, ox, oy, oz, s);
    }

    private static Rig stalker(StalkerModel<?> m, Identifier tex, float s) {
        ModelPart root = m.getPart();
        ModelPart body = root.getChild("body");
        ModelTransform bt = body.getDefaultTransform();
        Rig r = new Rig(tex, m, s, 0.72f * s, 0.28f * s, 0.18f * s);
        // Rumpf-Ursprung = Drehpunkt des Körpers; Kinder (Kopf, Arme) haben Koordinaten relativ dazu.
        Segment torso = new Segment(null, new Vector3f(), new Vector3f(0, 1, 0), 0.8f * s);
        torso.parts.add(body);
        ModelPart head = body.getChild("head"), la = body.getChild("left_arm"), ra = body.getChild("right_arm");
        torso.hiddenChildren.add(head); torso.hiddenChildren.add(la); torso.hiddenChildren.add(ra);
        Segment h = new Segment(Limb.HEAD, childPivot(head, s), new Vector3f(0, 1, 0), 0.6f * s);
        h.parts.add(head);
        Segment l = new Segment(Limb.LEFT_ARM, childPivot(la, s), new Vector3f(0, -1, 0), 1.0f * s);
        l.parts.add(la);
        Segment rr = new Segment(Limb.RIGHT_ARM, childPivot(ra, s), new Vector3f(0, -1, 0), 1.0f * s);
        rr.parts.add(ra);
        ModelPart ll = root.getChild("left_leg"), rl = root.getChild("right_leg");
        Segment lleg = new Segment(Limb.LEFT_LEG, pivot(ll, bt.pivotX, bt.pivotY, bt.pivotZ, s), new Vector3f(0, -1, 0), 1.0f * s);
        lleg.parts.add(ll);
        Segment rleg = new Segment(Limb.RIGHT_LEG, pivot(rl, bt.pivotX, bt.pivotY, bt.pivotZ, s), new Vector3f(0, -1, 0), 1.0f * s);
        rleg.parts.add(rl);
        r.torso = torso;
        r.limbs.add(h); r.limbs.add(l); r.limbs.add(rr); r.limbs.add(lleg); r.limbs.add(rleg);
        return r;
    }

    private static Vector3f childPivot(ModelPart p, float s) {
        ModelTransform t = p.getDefaultTransform();
        return local(t.pivotX, t.pivotY, t.pivotZ, 0, 0, 0, s);
    }
}
