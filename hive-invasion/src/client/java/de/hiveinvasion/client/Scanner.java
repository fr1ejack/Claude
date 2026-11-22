package de.hiveinvasion.client;

import de.hiveinvasion.entity.Alien;
import de.hiveinvasion.exo.ExoSuitEntity;
import de.hiveinvasion.net.ScanPayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Visier-Scanner im Exo-Anzug: Menschen werden umrandet – grün = gesund, rot = infiziert.
 * Aliens orange, infizierte Tiere rot. Funktioniert auch durch Wände.
 */
public final class Scanner {
    public static boolean enabled = true;
    private static final IntOpenHashSet INFECTED = new IntOpenHashSet();

    public static final int GREEN = 0x3CFF5A, RED = 0xFF2A2A, ORANGE = 0xFF9A1F;

    private Scanner() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ScanPayload.ID, (payload, ctx) -> {
            INFECTED.clear();
            INFECTED.addAll(payload.infected());
        });
    }

    public static boolean active() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return enabled && mc.player != null && mc.player.getVehicle() instanceof ExoSuitEntity suit && suit.isClosed();
    }

    public static int infectedCount() { return INFECTED.size(); }

    /** Farbe der Umrandung oder -1 = nicht markieren. */
    public static int colorFor(Entity e) {
        if (!active() || !(e instanceof LivingEntity living) || !living.isAlive()) return -1;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e == mc.player || e instanceof ExoSuitEntity || e.squaredDistanceTo(mc.player) > 48 * 48) return -1;
        boolean infected = INFECTED.contains(e.getId());
        if (e instanceof Alien) return ORANGE;
        if (e instanceof PlayerEntity || e instanceof MerchantEntity || isHuman(e)) return infected ? RED : GREEN;
        return infected ? RED : -1;
    }

    private static boolean isHuman(Entity e) {
        String path = net.minecraft.registry.Registries.ENTITY_TYPE.getId(e.getType()).getPath();
        return path.contains("soldier") || path.contains("civilian");
    }
}
