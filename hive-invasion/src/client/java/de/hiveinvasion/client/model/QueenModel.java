package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.hive.QueenEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Königin: atmet, peitscht mit dem Schwanz, Eiersack pulsiert beim Eierlegen. */
public class QueenModel extends SinglePartEntityModel<QueenEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("queen"), "main");
    private final ModelPart root, body, head, jaw, sac, leftArm, rightArm, leftSmall, rightSmall;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[8];

    public QueenModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        sac = body.getChild("sac");
        leftArm = body.getChild("left_arm"); rightArm = body.getChild("right_arm");
        leftSmall = body.getChild("left_small"); rightSmall = body.getChild("right_small");
        legs[0] = root.getChild("left_leg"); legs[1] = root.getChild("right_leg");
        legs[2] = root.getChild("left_hind"); legs[3] = root.getChild("right_hind");
        ModelPart t = body.getChild("tail0");
        for (int i = 0; i < 8; i++) { tail[i] = t; if (i < 7) t = t.getChild("tail" + (i + 1)); }
    }

    public static TexturedModelData getTexturedModelData() { return GenModels.queen(); }

    @Override public ModelPart getPart() { return root; }

    @Override
    public void setAngles(QueenEntity q, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        float breathe = MathHelper.sin(age * 0.05f);
        body.pitch += breathe * 0.03f;
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.6f;
        head.pitch += headPitch * MathHelper.RADIANS_PER_DEGREE * 0.4f;
        jaw.pitch += Math.max(0, MathHelper.sin(age * 0.07f)) * 0.3f;
        float swing = MathHelper.cos(limbAngle * 0.4f) * 0.6f * limbDistance;
        legs[0].pitch += swing; legs[1].pitch -= swing; legs[2].pitch -= swing; legs[3].pitch += swing;
        leftArm.pitch += -swing * 0.5f + MathHelper.sin(age * 0.04f) * 0.05f;
        rightArm.pitch += swing * 0.5f - MathHelper.sin(age * 0.04f) * 0.05f;
        leftSmall.pitch += MathHelper.sin(age * 0.11f) * 0.15f;
        rightSmall.pitch += MathHelper.sin(age * 0.11f + 1) * 0.15f;
        for (int i = 0; i < 8; i++) tail[i].yaw = MathHelper.sin(age * 0.05f - i * 0.5f) * (0.08f + i * 0.02f);
        // Eiersack: pulsiert, beim Legen stark
        float lay = q.getLaying() / (float) QueenEntity.LAY_TIME;
        float pulse = 1 + 0.03f * MathHelper.sin(age * 0.15f) + lay * 0.12f * MathHelper.sin(age * 0.6f);
        sac.xScale = pulse; sac.yScale = pulse; sac.zScale = 1 + lay * 0.08f;
    }
}
