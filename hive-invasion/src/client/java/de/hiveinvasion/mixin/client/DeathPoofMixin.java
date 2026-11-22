package de.hiveinvasion.mixin.client;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keine weiße "Puff"-Wolke beim Tod – der Körper bleibt ja als Leiche liegen. */
@Mixin(LivingEntity.class)
public class DeathPoofMixin {
    @Inject(method = "addDeathParticles", at = @At("HEAD"), cancellable = true)
    private void hiveinvasion$noPoof(CallbackInfo ci) {
        ci.cancel();
    }
}
