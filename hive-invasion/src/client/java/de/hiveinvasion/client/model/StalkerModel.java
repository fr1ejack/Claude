package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Gebückt laufendes Alien mit langem Hinterkopf, langen Armen und Schwanz. */
public class StalkerModel<T extends Entity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("stalker"), "main");

    private final ModelPart root, body, head, jaw, leftArm, rightArm, leftLeg, rightLeg, tail1, tail2, tail3;

    public StalkerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = head.getChild("jaw");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.tail1 = body.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();

        r.addChild("left_leg", ModelPartBuilder.create().uv(0, 32).cuboid(-1.5f, 0, -1.5f, 3, 12, 3),
                ModelTransform.pivot(2.5f, 12, 1));
        r.addChild("right_leg", ModelPartBuilder.create().uv(12, 32).mirrored().cuboid(-1.5f, 0, -1.5f, 3, 12, 3),
                ModelTransform.pivot(-2.5f, 12, 1));

        ModelPartData body = r.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4, -11, -2.5f, 8, 11, 5),
                ModelTransform.of(0, 12, 1, 0.45f, 0, 0));

        // Röhren auf dem Rücken
        body.addChild("spine1", ModelPartBuilder.create().uv(28, 48).cuboid(-0.5f, -0.5f, 0, 1, 1, 4),
                ModelTransform.of(-2.5f, -9, 2.5f, 0.6f, -0.3f, 0));
        body.addChild("spine2", ModelPartBuilder.create().uv(38, 48).cuboid(-0.5f, -0.5f, 0, 1, 1, 4),
                ModelTransform.of(2.5f, -9, 2.5f, 0.6f, 0.3f, 0));
        body.addChild("spine3", ModelPartBuilder.create().uv(48, 48).cuboid(-0.5f, -0.5f, 0, 1, 1, 4),
                ModelTransform.of(0, -6, 2.5f, 0.4f, 0, 0));

        ModelPartData head = body.addChild("head", ModelPartBuilder.create()
                        .uv(28, 0).cuboid(-2.5f, -3, -6, 5, 4, 6)        // Gesicht
                        .uv(0, 16).cuboid(-2, -4.5f, -2, 4, 4, 9),       // langer Hinterkopf
                ModelTransform.of(0, -11, -1, -0.45f, 0, 0));
        head.addChild("jaw", ModelPartBuilder.create().uv(50, 0).cuboid(-1.5f, 0, -5.5f, 3, 1, 5),
                ModelTransform.pivot(0, 1, 0));

        body.addChild("left_arm", ModelPartBuilder.create().uv(48, 16).cuboid(-1, -1, -1, 2, 14, 2),
                ModelTransform.of(5, -10, 0, -0.6f, 0, -0.1f));
        body.addChild("right_arm", ModelPartBuilder.create().uv(56, 16).mirrored().cuboid(-1, -1, -1, 2, 14, 2),
                ModelTransform.of(-5, -10, 0, -0.6f, 0, 0.1f));

        ModelPartData t1 = body.addChild("tail1", ModelPartBuilder.create().uv(26, 16).cuboid(-1, -1, 0, 2, 2, 7),
                ModelTransform.of(0, -1, 2, -0.9f, 0, 0));
        ModelPartData t2 = t1.addChild("tail2", ModelPartBuilder.create().uv(26, 25).cuboid(-1, -1, 0, 2, 2, 7),
                ModelTransform.of(0, 0, 6.5f, 0.3f, 0, 0));
        ModelPartData t3 = t2.addChild("tail3", ModelPartBuilder.create().uv(24, 36).cuboid(-0.5f, -0.5f, 0, 1, 1, 7),
                ModelTransform.of(0, 0, 6.5f, 0.3f, 0, 0));
        t3.addChild("blade", ModelPartBuilder.create().uv(44, 36).cuboid(-0.5f, -1.5f, 0, 1, 3, 4),
                ModelTransform.pivot(0, 0, 6.5f));

        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = -0.45f + headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        jaw.pitch = 0.1f + MathHelper.sin(age * 0.1f) * 0.08f;

        float swing = MathHelper.cos(limbAngle * 0.6662f) * 1.3f * limbDistance;
        leftLeg.pitch = swing;
        rightLeg.pitch = -swing;
        leftArm.pitch = -0.6f - swing * 0.8f;
        rightArm.pitch = -0.6f + swing * 0.8f;

        float sway = MathHelper.sin(age * 0.08f);
        tail1.yaw = sway * 0.25f;
        tail2.yaw = sway * 0.3f;
        tail3.yaw = sway * 0.35f;
        tail1.pitch = -0.9f + MathHelper.sin(age * 0.05f) * 0.05f;
    }
}
