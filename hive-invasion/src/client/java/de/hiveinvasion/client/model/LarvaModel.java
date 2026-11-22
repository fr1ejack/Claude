package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Wurmartige Larve mit Kopf voller Zähne. */
public class LarvaModel<T extends Entity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("larva"), "main");

    private final ModelPart root, head, mid, back;

    public LarvaModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.mid = body.getChild("mid");
        this.back = mid.getChild("back");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData body = data.getRoot().addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0, 24, 0));
        body.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-1.5f, -3.5f, -4, 3, 3.5f, 4),
                ModelTransform.pivot(0, 0, -2));
        ModelPartData mid = body.addChild("mid", ModelPartBuilder.create().uv(0, 8).cuboid(-1.5f, -3, 0, 3, 3, 5),
                ModelTransform.pivot(0, 0, -2));
        mid.addChild("back", ModelPartBuilder.create().uv(0, 17).cuboid(-1, -2, 0, 2, 2, 5),
                ModelTransform.pivot(0, 0, 5));
        return TexturedModelData.of(data, 32, 32);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        float wiggle = MathHelper.sin(limbAngle * 1.2f) * limbDistance + MathHelper.sin(age * 0.2f) * 0.1f;
        head.yaw = wiggle * 0.4f;
        mid.yaw = -wiggle * 0.3f;
        back.yaw = wiggle * 0.5f;
        head.pitch = -0.1f + MathHelper.sin(age * 0.15f) * 0.08f;
    }
}
