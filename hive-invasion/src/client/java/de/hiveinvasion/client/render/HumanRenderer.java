package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.human.CivilianEntity;
import de.hiveinvasion.human.HumanEntity;
import de.hiveinvasion.human.SoldierEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

/** Zivilisten und Soldaten mit Spielermodell; Soldaten halten das Gewehr im Anschlag. */
public class HumanRenderer extends BipedEntityRenderer<HumanEntity, HumanRenderer.Model> {
    public HumanRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new Model(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(HumanEntity e) {
        String kind = e instanceof SoldierEntity ? "soldier" : "civilian";
        return HiveInvasion.id("textures/entity/human/" + kind + "_" + e.getVariant() + ".png");
    }

    @Override
    protected void scale(HumanEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(0.9375f, 0.9375f, 0.9375f);
    }

    public static class Model extends PlayerEntityModel<HumanEntity> {
        public Model(net.minecraft.client.model.ModelPart root) { super(root, false); }

        @Override
        public void setAngles(HumanEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
            super.setAngles(e, limbAngle, limbDistance, age, headYaw, headPitch);
            if (e instanceof SoldierEntity s && s.isAiming()) {
                CrossbowPosing.hold(rightArm, leftArm, head, true);
            } else if (e instanceof SoldierEntity) {
                // Gewehr schräg vor der Brust
                rightArm.pitch = -0.6f + rightArm.pitch * 0.3f;
                rightArm.yaw = -0.3f;
                leftArm.pitch = -0.9f + leftArm.pitch * 0.3f;
                leftArm.yaw = 0.6f;
            } else if (e instanceof CivilianEntity && e.hurtTime > 0) {
                rightArm.pitch = -2.6f; leftArm.pitch = -2.6f;   // Arme hoch vor Schreck
            }
            leftSleeve.copyTransform(leftArm);
            rightSleeve.copyTransform(rightArm);
        }
    }
}
