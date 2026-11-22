package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Gebückt laufendes Alien: Vogelbeine, lange Arme mit Krallen, langer Schädel, Schwanz mit Klinge. */
public class StalkerModel<T extends Entity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("stalker"), "main");

    private final ModelPart root, body, head, jaw, leftArm, rightArm, leftForearm, rightForearm, leftLeg, rightLeg;
    private final ModelPart[] tail = new ModelPart[6];
    private final float bodyPitch, headPitch, armPitch, forearmPitch, legPitch;

    public StalkerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        ModelPart t = body.getChild("tail0");
        for (int i = 0; i < 6; i++) {
            tail[i] = t;
            if (i < 5) t = t.getChild("tail" + (i + 1));
        }
        bodyPitch = body.pitch; headPitch = head.pitch; armPitch = leftArm.pitch;
        forearmPitch = leftForearm.pitch; legPitch = leftLeg.pitch;
    }

    public static TexturedModelData getTexturedModelData() {
        return GenModels.stalker();
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitchDeg) {
        float breathe = MathHelper.sin(age * 0.06f);
        body.pitch = bodyPitch + breathe * 0.03f + limbDistance * 0.25f;   // beim Rennen tiefer geduckt
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = headPitch + headPitchDeg * MathHelper.RADIANS_PER_DEGREE * 0.5f - limbDistance * 0.25f;
        jaw.pitch = 0.14f + Math.max(0, MathHelper.sin(age * 0.09f)) * 0.25f;

        float swing = MathHelper.cos(limbAngle * 0.6662f) * 1.1f * limbDistance;
        leftLeg.pitch = legPitch + swing;
        rightLeg.pitch = legPitch - swing;
        leftArm.pitch = armPitch - swing * 0.7f;
        rightArm.pitch = armPitch + swing * 0.7f;
        leftForearm.pitch = forearmPitch - Math.max(0, swing) * 0.5f;
        rightForearm.pitch = forearmPitch - Math.max(0, -swing) * 0.5f;

        int mask = de.hiveinvasion.client.ClientGore.mask(entity);
        head.visible = !de.hiveinvasion.gore.Limb.HEAD.in(mask);
        leftArm.visible = !de.hiveinvasion.gore.Limb.LEFT_ARM.in(mask);
        rightArm.visible = !de.hiveinvasion.gore.Limb.RIGHT_ARM.in(mask);
        leftLeg.visible = !de.hiveinvasion.gore.Limb.LEFT_LEG.in(mask);
        rightLeg.visible = !de.hiveinvasion.gore.Limb.RIGHT_LEG.in(mask);

        for (int i = 0; i < 6; i++) {
            float phase = age * 0.09f - i * 0.6f;
            tail[i].yaw = MathHelper.sin(phase) * (0.12f + i * 0.03f) * (1 - limbDistance * 0.5f);
        }
    }
}
