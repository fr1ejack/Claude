package de.hiveinvasion.mixin.client;

import de.hiveinvasion.exo.ExoSuitEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spieler im geschlossenen Exo-Anzug werden nicht gezeichnet – man sieht nur den Anzug. */
@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void hiveinvasion$hideInSuit(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                                         VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (player.getVehicle() instanceof ExoSuitEntity) ci.cancel();
    }
}
