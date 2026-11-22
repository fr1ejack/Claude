package de.hiveinvasion.client.render;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

public class AlienRenderer<T extends MobEntity, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
    private final Identifier texture;
    private final float scale;

    public AlienRenderer(EntityRendererFactory.Context ctx, M model, float shadow, String name, float scale) {
        super(ctx, model, shadow);
        this.texture = HiveInvasion.id("textures/entity/" + name + ".png");
        this.scale = scale;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }
}
