package de.hiveinvasion.client.model;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.exo.ExoRackEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;

public class ExoRackModel extends SinglePartEntityModel<ExoRackEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(HiveInvasion.id("exo_rack"), "main");
    private final ModelPart root;

    public ExoRackModel(ModelPart root) { this.root = root; }

    public static TexturedModelData getTexturedModelData() { return GenModels.exorack(); }

    @Override public ModelPart getPart() { return root; }

    @Override
    public void setAngles(ExoRackEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {}
}
