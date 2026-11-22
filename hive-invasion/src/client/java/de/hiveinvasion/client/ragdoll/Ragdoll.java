package de.hiveinvasion.client.ragdoll;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Einfache Ragdoll-Physik für eine Leiche.
 *
 * Rumpf: kippt wie ein umfallender Körper um die Füße (Winkel θ), landet und rollt dann
 * um die eigene Längsachse auf Rücken oder Bauch (Winkel φ) – die Seitenlage ist instabil.
 * Glieder: Verlet-Punkte an Gelenken mit fester Länge, Schwerkraft, Bodenkontakt mit Reibung
 * und begrenztem Bewegungsbereich (Kopf steif, Beine mittel, Arme locker).
 * Alles im Koordinatensystem der Leiche (Ursprung = Füße beim Tod, y nach oben).
 */
public final class Ragdoll {
    private static final float G = 22f;          // Blöcke/s²
    private static final float DT = 0.05f / 4;    // 4 Unterschritte pro Tick

    public final Rig rig;
    private final float fallYaw;      // Richtung, in die der Körper fällt
    private float theta, thetaVel;    // Kippwinkel (0 = steht, π/2 = liegt)
    private float phi, phiVel;        // Rollen um die Längsachse (0 = Seite, ±π/2 = Rücken/Bauch)
    private final float rollStrength;
    private final Vector3f pelvis = new Vector3f();
    private final Quaternionf torsoRot = new Quaternionf();
    private final Vector3f[] tip, oldTip;
    private int restTicks;
    private final Vector3f prevPelvis = new Vector3f();
    private final Quaternionf prevTorso = new Quaternionf();
    private Vector3f[] prevTip;
    public boolean asleep;

    public Ragdoll(Rig rig, long seed, float fallYawDeg) {
        this.rig = rig;
        Random r = Random.create(seed);
        this.fallYaw = fallYawDeg * MathHelper.RADIANS_PER_DEGREE;
        this.theta = 0.05f + r.nextFloat() * 0.08f;
        this.thetaVel = 0.6f + r.nextFloat() * 1.2f;
        this.phi = (r.nextFloat() - 0.5f) * 0.3f;
        this.phiVel = (r.nextFloat() - 0.5f) * 1.5f;
        // Meist rollt der Körper weiter auf Rücken oder Bauch, manchmal bleibt er auf der Seite liegen.
        this.rollStrength = (!rig.isRigid && r.nextFloat() < 0.8f) ? 14f : -6f;
        updateTorso();
        int n = rig.limbs.size();
        tip = new Vector3f[n]; oldTip = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Rig.Segment s = rig.limbs.get(i);
            Vector3f j = jointWorld(s);
            Vector3f t = new Vector3f(s.rest).rotate(torsoRot).mul(s.length).add(j);
            // etwas Zufall, damit nicht alle Leichen gleich fallen
            t.add((r.nextFloat() - 0.5f) * 0.1f, 0, (r.nextFloat() - 0.5f) * 0.1f);
            tip[i] = t; oldTip[i] = new Vector3f(t);
        }
        snapshot();
    }

    public Quaternionf torsoRotation() { return torsoRot; }
    public Vector3f pelvis() { return pelvis; }

    public Vector3f jointWorld(Rig.Segment s) {
        return new Vector3f(s.joint).rotate(torsoRot).add(pelvis);
    }

    public Vector3f tip(int i) { return tip[i]; }

    /** Drehung, die das Glied i von seiner Ruhelage in die aktuelle Richtung bringt. */
    public Quaternionf limbRotation(int i) {
        Rig.Segment s = rig.limbs.get(i);
        Vector3f restWorld = new Vector3f(s.rest).rotate(torsoRot);
        Vector3f dir = new Vector3f(tip[i]).sub(jointWorld(s)).normalize();
        return new Quaternionf().rotationTo(restWorld, dir).mul(torsoRot);
    }

    private void updateTorso() {
        float spin = MathHelper.HALF_PI + phi;   // Seite zur Fallrichtung → landet auf der Seite
        torsoRot.identity().rotateY(fallYaw).rotateX(theta).rotateY(spin);
        // Becken kippt um die Füße
        Vector3f up = new Vector3f(0, rig.legLength, 0).rotateX(theta).rotateY(fallYaw);
        float lying = MathHelper.clamp(theta / MathHelper.HALF_PI, 0, 1);
        float thickness = MathHelper.lerp(Math.abs(MathHelper.sin(phi)), rig.halfWidth, rig.halfDepth);
        pelvis.set(up.x, Math.max(up.y, thickness * lying), up.z);
    }

    /** Stoß (z. B. Treffer oder Schieben), in Blöcken pro Tick. */
    public void impulse(float x, float y, float z) {
        asleep = false; restTicks = 0;
        for (int i = 0; i < tip.length; i++) {
            float k = 0.6f + 0.4f * (i % 2);
            oldTip[i].sub(x * k, y * k, z * k);
        }
        phiVel += (x + z) * 3f;
    }

    private void snapshot() {
        prevPelvis.set(pelvis);
        prevTorso.set(torsoRot);
        if (prevTip == null) prevTip = new Vector3f[tip.length];
        for (int i = 0; i < tip.length; i++) {
            if (prevTip[i] == null) prevTip[i] = new Vector3f();
            prevTip[i].set(tip[i]);
        }
    }

    /** Zwischenstellung zwischen zwei Ticks für flüssige Darstellung. */
    public Pose pose(float delta) {
        Pose pose = new Pose();
        pose.pelvis = new Vector3f(prevPelvis).lerp(pelvis, delta);
        pose.torso = new Quaternionf(prevTorso).slerp(torsoRot, delta);
        pose.limbRot = new Quaternionf[tip.length];
        pose.joint = new Vector3f[tip.length];
        for (int i = 0; i < tip.length; i++) {
            Rig.Segment s = rig.limbs.get(i);
            Vector3f j = new Vector3f(s.joint).rotate(pose.torso).add(pose.pelvis);
            Vector3f t = new Vector3f(prevTip[i]).lerp(tip[i], delta);
            Vector3f restWorld = new Vector3f(s.rest).rotate(pose.torso);
            Vector3f dir = t.sub(j).normalize();
            pose.joint[i] = j;
            pose.limbRot[i] = new Quaternionf().rotationTo(restWorld, dir).mul(pose.torso);
        }
        return pose;
    }

    public static final class Pose {
        public Vector3f pelvis;
        public Quaternionf torso;
        public Vector3f[] joint;
        public Quaternionf[] limbRot;
    }

    public void tick() {
        snapshot();
        if (asleep) return;
        float motion = 0;
        for (int step = 0; step < 4; step++) motion += substep();
        if (motion < 0.002f && theta >= MathHelper.HALF_PI - 0.01f && Math.abs(phiVel) < 0.05f) {
            if (++restTicks > 40) asleep = true;
        } else restTicks = 0;
    }

    private float substep() {
        // --- Rumpf
        if (theta < MathHelper.HALF_PI) {
            thetaVel += (G / Math.max(0.4f, rig.legLength + 0.4f)) * MathHelper.sin(theta) * DT;
            theta += thetaVel * DT;
            if (theta >= MathHelper.HALF_PI) {
                theta = MathHelper.HALF_PI;
                thetaVel = -thetaVel * 0.18f;   // kleiner Aufprall
                phiVel += (phi >= 0 ? 1 : -1) * 1.2f;
            }
        } else if (thetaVel < 0) {
            theta += thetaVel * DT;
            thetaVel += 30f * DT;
            if (theta >= MathHelper.HALF_PI) { theta = MathHelper.HALF_PI; thetaVel = 0; }
        }
        if (theta > 1.2f) {
            phiVel += rollStrength * MathHelper.sin(2 * phi) * DT;
            phiVel *= 0.985f;
            phi += phiVel * DT;
            float lim = MathHelper.HALF_PI;
            if (phi > lim) { phi = lim; phiVel = -phiVel * 0.2f; }
            if (phi < -lim) { phi = -lim; phiVel = -phiVel * 0.2f; }
        }
        updateTorso();

        // --- Glieder
        float motion = 0;
        for (int i = 0; i < tip.length; i++) {
            Rig.Segment s = rig.limbs.get(i);
            Vector3f p = tip[i], o = oldTip[i];
            float vx = (p.x - o.x) * 0.985f, vy = (p.y - o.y) * 0.985f, vz = (p.z - o.z) * 0.985f;
            o.set(p);
            p.add(vx, vy - G * DT * DT, vz);
            Vector3f j = jointWorld(s);
            // Länge halten
            Vector3f d = new Vector3f(p).sub(j);
            float len = d.length();
            if (len < 1e-4f) d.set(s.rest).rotate(torsoRot); else d.div(len);
            // Bewegungsbereich begrenzen
            Vector3f rest = new Vector3f(s.rest).rotate(torsoRot);
            float maxAngle = s.limb == null ? 0 : switch (s.limb) {
                case HEAD -> 0.75f;
                case RIGHT_LEG, LEFT_LEG -> 1.6f;
                default -> 2.7f;
            };
            float angle = rest.angle(d);
            if (angle > maxAngle) {
                Quaternionf q = new Quaternionf().rotationTo(rest, d);
                Quaternionf back = new Quaternionf().slerp(q, maxAngle / angle);
                d.set(rest).rotate(back);
            }
            p.set(d).mul(s.length).add(j);
            // Boden
            float r = 0.08f;
            if (p.y < r) {
                p.y = r;
                o.x = MathHelper.lerp(0.35f, o.x, p.x);
                o.z = MathHelper.lerp(0.35f, o.z, p.z);
                o.y = p.y;
            }
            motion += Math.abs(p.x - o.x) + Math.abs(p.y - o.y) + Math.abs(p.z - o.z);
        }
        return motion;
    }

    /** Sofort bis zum Ruhezustand durchrechnen (für Leichen, die schon länger liegen). */
    public void settle() {
        for (int i = 0; i < 200 && !asleep; i++) tick();
        snapshot();
    }
}
