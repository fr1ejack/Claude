package de.hiveinvasion.mixin.client;

import de.hiveinvasion.client.ClientGore;
import de.hiveinvasion.entity.CorpseEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1. Fehlende Körperteile ausblenden.
 * 2. Die normale Sterbe-Animation nicht zeigen, wenn schon eine Ragdoll-Leiche da ist.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow public abstract M getModel();

    @Unique private boolean[] hiveinvasion$saved;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void hiveinvasion$before(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                     VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (entity.deathTime > 0 && !entity.getWorld().getEntitiesByClass(CorpseEntity.class,
                entity.getBoundingBox().expand(1.0), c -> true).isEmpty()) {
            ci.cancel();
            return;
        }
        hiveinvasion$saved = null;
        int mask = ClientGore.mask(entity);
        if (mask != 0 && getModel() instanceof BipedEntityModel<?> biped) {
            hiveinvasion$saved = ClientGore.hide(biped, mask);
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN"))
    private void hiveinvasion$after(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                    VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (hiveinvasion$saved != null && getModel() instanceof BipedEntityModel<?> biped) {
            ClientGore.restore(biped, hiveinvasion$saved);
            hiveinvasion$saved = null;
        }
    }
}
