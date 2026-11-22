package de.hiveinvasion.mixin.client;

import de.hiveinvasion.client.Scanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scanner: Umrisse durch Wände sichtbar machen. */
@Mixin(MinecraftClient.class)
public class OutlineMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void hiveinvasion$scan(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Scanner.colorFor(entity) != -1) cir.setReturnValue(true);
    }
}
