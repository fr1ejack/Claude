package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.exo.ExoSuitEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Exo-Anzug. Offen: Brustplatten aufgeklappt, Visier hochgeklappt, Arme und Beine gespreizt.
 * Beim Anlegen schließen sich nacheinander Beine → Brust → Arme → Helm.
 */
public class ExoSuitModel extends SinglePartEntityModel<ExoSuitEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("exo_suit"), "main");

    private final ModelPart root, body, chestLeft, chestRight, helmet, visor, leftArm, rightArm,
            leftForearm, rightForearm, leftLeg, rightLeg, leftShin, rightShin;

    public ExoSuitModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        chestLeft = body.getChild("chest_left");
        chestRight = body.getChild("chest_right");
        helmet = body.getChild("helmet");
        visor = helmet.getChild("visor");
        leftArm = body.getChild("left_arm");
        rightArm = body.getChild("right_arm");
        leftForearm = leftArm.getChild("left_forearm");
        rightForearm = rightArm.getChild("right_forearm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        leftShin = leftLeg.getChild("left_shin");
        rightShin = rightLeg.getChild("right_shin");
    }

    public static TexturedModelData getTexturedModelData() {
        return GenModels.exosuit();
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    private static float stage(float t, float start, float length) {
        float x = MathHelper.clamp((t - start) / length, 0, 1);
        return x * x * (3 - 2 * x);   // weich ein- und ausblenden
    }

    @Override
    public void setAngles(ExoSuitEntity suit, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        float tickDelta = age - suit.age;
        float t = suit.getAssembly(tickDelta);
        float legs = 1 - stage(t, 0.00f, 0.25f);
        float chest = 1 - stage(t, 0.22f, 0.28f);
        float arms = 1 - stage(t, 0.45f, 0.25f);
        float helm = 1 - stage(t, 0.68f, 0.30f);

        leftLeg.roll = -0.18f * legs;
        rightLeg.roll = 0.18f * legs;
        leftShin.pitch = 0.25f * legs;
        rightShin.pitch = 0.25f * legs;

        chestLeft.yaw = -1.75f * chest;
        chestRight.yaw = 1.75f * chest;

        leftArm.roll = -0.65f * arms;
        rightArm.roll = 0.65f * arms;
        leftArm.pitch = -0.25f * arms;
        rightArm.pitch = -0.25f * arms;
        leftForearm.pitch = -0.5f * arms;
        rightForearm.pitch = -0.5f * arms;

        visor.pitch = -1.9f * helm;
        helmet.pitch = 0.12f * helm;   // offen leicht nach hinten gekippt
        helmet.yaw = 0;

        if (t >= 1) {
            // Gehen
            float swing = MathHelper.cos(limbAngle * 0.5f) * 0.9f * limbDistance;
            leftLeg.pitch = swing;
            rightLeg.pitch = -swing;
            leftArm.pitch = -swing * 0.5f;
            rightArm.pitch = -0.15f + swing * 0.3f;   // Waffenarm ruhiger
            helmet.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.6f;
            helmet.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.4f;
            if (suit.isShieldActive()) {
                leftArm.pitch = -1.35f;
                leftArm.yaw = 0.35f;
            } else {
                leftArm.yaw = 0;
            }
            // Schussarm zeigt in Blickrichtung
            rightArm.pitch = -1.4f + headPitch * MathHelper.RADIANS_PER_DEGREE;
            rightForearm.pitch = 0;
        } else {
            leftLeg.pitch = rightLeg.pitch = 0;
            leftArm.yaw = 0;
        }
    }
}
