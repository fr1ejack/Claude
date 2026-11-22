package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.hive.CocoonEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Mensch an der Nestwand: Arme über dem Kopf, in Harz eingeschlossen, zappelt ab und zu. */
public class CocoonRenderer extends EntityRenderer<CocoonEntity> {
    public static final EntityModelLayer SHELL = new EntityModelLayer(HiveInvasion.id("cocoon"), "main");
    private static final Identifier SHELL_TEX = HiveInvasion.id("textures/entity/cocoon.png");
    private final PlayerEntityModel<CocoonEntity> human;
    private final ModelPart shell;

    public CocoonRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        human = new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false);
        shell = ctx.getPart(SHELL);
        shadowRadius = 0f;
    }

    @Override
    public void render(CocoonEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light) {
        m.push();
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - e.bodyYaw));
        float wiggle = e.getStruggle() > 0 ? MathHelper.sin((e.age + tickDelta) * 1.3f) * 0.06f : 0;
        m.multiply(RotationAxis.POSITIVE_Z.rotation(wiggle));
        m.scale(-1, -1, 1);
        m.translate(0, -1.501, 0);
        for (ModelPart p : new ModelPart[]{human.head, human.hat, human.body, human.jacket, human.leftArm, human.rightArm,
                human.leftSleeve, human.rightSleeve, human.leftLeg, human.rightLeg, human.leftPants, human.rightPants}) p.resetTransform();
        human.rightArm.pitch = (float) Math.PI; human.leftArm.pitch = (float) Math.PI;
        human.rightArm.roll = -0.15f; human.leftArm.roll = 0.15f;
        human.head.pitch = 0.25f + wiggle * 2; human.head.yaw = wiggle * 4;
        human.hat.copyTransform(human.head);
        human.rightSleeve.copyTransform(human.rightArm); human.leftSleeve.copyTransform(human.leftArm);
        Identifier skin = HiveInvasion.id("textures/entity/human/civilian_" + e.getVariant() + ".png");
        human.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(skin)), light, OverlayTexture.DEFAULT_UV);
        shell.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(SHELL_TEX)), light, OverlayTexture.DEFAULT_UV);
        m.pop();
        super.render(e, yaw, tickDelta, m, vcp, light);
    }

    @Override public Identifier getTexture(CocoonEntity e) { return SHELL_TEX; }
}
