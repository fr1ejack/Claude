package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Wurmartige Larve mit Stummelärmchen und Zähnen. */
public class LarvaModel<T extends Entity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("larva"), "main");

    private final ModelPart root, head, mid, back, tip;

    public LarvaModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        head = body.getChild("head");
        mid = body.getChild("mid");
        back = mid.getChild("back");
        tip = back.getChild("tip");
    }

    public static TexturedModelData getTexturedModelData() {
        return GenModels.larva();
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        float w = MathHelper.sin(limbAngle * 1.3f) * limbDistance * 1.2f + MathHelper.sin(age * 0.2f) * 0.08f;
        head.yaw = w * 0.4f;
        mid.yaw = -w * 0.25f;
        back.yaw = w * 0.45f;
        tip.yaw = -w * 0.5f;
        head.pitch = -0.15f + MathHelper.sin(age * 0.17f) * 0.1f;
    }
}
