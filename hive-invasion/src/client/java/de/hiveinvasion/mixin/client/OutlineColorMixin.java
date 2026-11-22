package de.hiveinvasion.mixin.client;

import de.hiveinvasion.client.Scanner;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scanner: Farbe der Umrandung (grün/rot/orange). */
@Mixin(Entity.class)
public class OutlineColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void hiveinvasion$color(CallbackInfoReturnable<Integer> cir) {
        int c = Scanner.colorFor((Entity) (Object) this);
        if (c != -1) cir.setReturnValue(c);
    }
}
