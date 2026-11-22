package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.exo.ExoSuitEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Exo-Anzug mit aufklappbaren Schalen.
 * Offen (im Gestell): Stiefel unten aufgeklappt, Beinschalen offen, Arme waagerecht zur Seite
 * mit offenen Schalen und Handschuhen außen, Brustplatten offen, Helm oben am Kran.
 * Beim Anlegen (Tick 0–120): Stiefel → Handschuhe → Armschalen → Beinschalen →
 * Arme senken + Brust → Helm senkt sich → Visier schließt.
 */
public class ExoSuitModel extends SinglePartEntityModel<ExoSuitEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("exo_suit"), "main");

    private final ModelPart root, body, chestLeft, chestRight, helmet, visor,
            leftArm, rightArm, leftArmFront, rightArmFront, leftForearm, rightForearm, leftGlove, rightGlove,
            leftLeg, rightLeg, leftThigh, rightThigh, leftShinFront, rightShinFront, leftBoot, rightBoot;

    public ExoSuitModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        chestLeft = body.getChild("chest_left");
        chestRight = body.getChild("chest_right");
        helmet = body.getChild("helmet");
        visor = helmet.getChild("visor");
        leftArm = body.getChild("left_arm");
        rightArm = body.getChild("right_arm");
        leftArmFront = leftArm.getChild("left_arm_front");
        rightArmFront = rightArm.getChild("right_arm_front");
        leftForearm = leftArm.getChild("left_forearm");
        rightForearm = rightArm.getChild("right_forearm");
        leftGlove = leftForearm.getChild("left_gauntlet_front");
        rightGlove = rightForearm.getChild("right_gauntlet_front");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        leftThigh = leftLeg.getChild("left_thigh_front");
        rightThigh = rightLeg.getChild("right_thigh_front");
        leftShinFront = leftLeg.getChild("left_shin").getChild("left_shin_front");
        rightShinFront = rightLeg.getChild("right_shin").getChild("right_shin_front");
        leftBoot = root.getChild("left_boot").getChild("left_boot_front");
        rightBoot = root.getChild("right_boot").getChild("right_boot_front");
    }

    public static TexturedModelData getTexturedModelData() {
        return GenModels.exosuit();
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    /** Weicher Übergang 0→1 zwischen Tick start und start+length. */
    public static float stage(float tick, float start, float length) {
        float x = MathHelper.clamp((tick - start) / length, 0, 1);
        return x * x * (3 - 2 * x);
    }

    @Override
    public void setAngles(ExoSuitEntity suit, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        float tickDelta = age - suit.age;
        float T = suit.getAssembly(tickDelta) * ExoSuitEntity.ASSEMBLE_TICKS;

        float boots = 1 - stage(T, 32, 8);
        float gloves = 1 - stage(T, 48, 7);
        float armShells = 1 - stage(T, 55, 15);
        float legShells = 1 - stage(T, 70, 15);
        float armsUp = 1 - stage(T, 85, 15);
        float chest = 1 - stage(T, 85, 15);
        float helmUp = 1 - stage(T, 100, 12);
        float visorOpen = 1 - stage(T, 110, 10);

        leftBoot.pitch = 1.15f * boots;
        rightBoot.pitch = 1.15f * boots;
        leftThigh.yaw = -1.7f * legShells;
        rightThigh.yaw = 1.7f * legShells;
        leftShinFront.yaw = -1.7f * legShells;
        rightShinFront.yaw = 1.7f * legShells;

        leftArm.roll = -1.5f * armsUp;
        rightArm.roll = 1.5f * armsUp;
        leftArmFront.yaw = -1.7f * armShells;
        rightArmFront.yaw = 1.7f * armShells;
        leftGlove.yaw = -1.7f * gloves;
        rightGlove.yaw = 1.7f * gloves;

        chestLeft.yaw = -1.9f * chest;
        chestRight.yaw = 1.9f * chest;

        helmet.pivotY -= 9f * helmUp;
        visor.pitch = -1.8f * visorOpen;

        if (T >= ExoSuitEntity.ASSEMBLE_TICKS) {
            float swing = MathHelper.cos(limbAngle * 0.55f) * 0.8f * limbDistance;
            leftLeg.pitch = swing;
            rightLeg.pitch = -swing;
            leftArm.pitch = -swing * 0.5f;
            helmet.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.7f;
            helmet.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
            if (suit.isShieldActive()) { leftArm.pitch = -1.35f; leftArm.yaw = 0.35f; }
            // Kanonenarm zeigt in Blickrichtung
            rightArm.pitch = -1.45f + headPitch * MathHelper.RADIANS_PER_DEGREE;
        }
    }
}
