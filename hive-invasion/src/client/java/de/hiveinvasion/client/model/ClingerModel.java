package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Flacher Körper mit acht langen, fingerartigen Beinen und einem Schwanz. */
public class ClingerModel<T extends Entity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("clinger"), "main");

    private final ModelPart root;
    private final ModelPart[] legs = new ModelPart[8];
    private final ModelPart tail;

    public ClingerModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        for (int i = 0; i < 8; i++) legs[i] = body.getChild("leg" + i);
        this.tail = body.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData body = data.getRoot().addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-3, -1.5f, -4, 6, 3, 7),
                ModelTransform.pivot(0, 21.5f, 0));
        for (int i = 0; i < 8; i++) {
            boolean left = i < 4;
            float z = -3 + (i % 4) * 1.8f;
            float yaw = (left ? -1 : 1) * (0.9f - (i % 4) * 0.45f);
            body.addChild("leg" + i, ModelPartBuilder.create().uv(0, 10 + (i % 4) * 2)
                            .cuboid(left ? 0 : -8, -0.5f, -0.5f, 8, 1, 1),
                    ModelTransform.of(left ? 3 : -3, 0.5f, z, 0, yaw, left ? 0.35f : -0.35f));
        }
        body.addChild("tail", ModelPartBuilder.create().uv(0, 20).cuboid(-0.5f, -0.5f, 0, 1, 1, 10),
                ModelTransform.of(0, 0, 3, -0.1f, 0, 0));
        return TexturedModelData.of(data, 32, 32);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        for (int i = 0; i < 8; i++) {
            boolean left = i < 4;
            float phase = limbAngle * 1.4f + i * 0.8f;
            legs[i].roll = (left ? 0.35f : -0.35f) + MathHelper.sin(phase) * 0.5f * limbDistance
                    + MathHelper.sin(age * 0.3f + i) * 0.05f;
        }
        tail.yaw = MathHelper.sin(age * 0.25f) * 0.4f;
    }
}
