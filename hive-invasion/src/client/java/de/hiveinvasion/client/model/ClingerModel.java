package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Flacher Körper mit acht zweigliedrigen Fingerbeinen und einem Schwanz. */
public class ClingerModel<T extends Entity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("clinger"), "main");

    private final ModelPart root;
    private final ModelPart[] legs = new ModelPart[8], tips = new ModelPart[8];
    private final float[] legRoll = new float[8];
    private final ModelPart tail, tail2, tail3;

    public ClingerModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        for (int i = 0; i < 8; i++) {
            legs[i] = body.getChild("leg" + i);
            tips[i] = legs[i].getChild("leg" + i + "_tip");
            legRoll[i] = legs[i].roll;
        }
        tail = body.getChild("tail");
        tail2 = tail.getChild("tail2");
        tail3 = tail2.getChild("tail3");
    }

    public static TexturedModelData getTexturedModelData() {
        return GenModels.clinger();
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        for (int i = 0; i < 8; i++) {
            float phase = limbAngle * 1.6f + i * 1.3f;
            float side = i < 4 ? 1 : -1;
            legs[i].roll = legRoll[i] + side * (MathHelper.sin(phase) * 0.45f * limbDistance
                    + MathHelper.sin(age * 0.35f + i) * 0.04f);
        }
        tail.yaw = MathHelper.sin(age * 0.25f) * 0.35f;
        tail2.yaw = MathHelper.sin(age * 0.25f - 0.8f) * 0.4f;
        tail3.yaw = MathHelper.sin(age * 0.25f - 1.6f) * 0.5f;
    }
}
