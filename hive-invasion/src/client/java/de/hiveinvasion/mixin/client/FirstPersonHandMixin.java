package de.hiveinvasion.mixin.client;

import de.hiveinvasion.exo.ExoSuitEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Im Exo-Anzug keine nackte Hand in der Ich-Perspektive – man sieht durchs Visier. */
@Mixin(HeldItemRenderer.class)
public class FirstPersonHandMixin {
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"), cancellable = true)
    private void hiveinvasion$hideInSuit(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate consumers,
                                         ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (player.getVehicle() instanceof ExoSuitEntity) ci.cancel();
    }
}
