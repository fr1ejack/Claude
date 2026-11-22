package de.hiveinvasion.mixin.client;

import de.hiveinvasion.client.ExoClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Kamerafahrt beim Anlegen des Anzugs: weiter weg, damit man die ganze Szene sieht. */
@Mixin(Camera.class)
public class CameraMixin {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"), index = 0)
    private float hiveinvasion$cutsceneDistance(float distance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && ExoClient.ASSEMBLING.contains(mc.player.getUuid()) ? distance * 1.4f : distance;
    }
}
