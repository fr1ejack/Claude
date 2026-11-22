package de.hiveinvasion.hive;

import de.hiveinvasion.entity.ClingerEntity;
import de.hiveinvasion.entity.StalkerEntity;
import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.world.Placer;
import de.hiveinvasion.world.WorldState;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

/**
 * Lebendiges Nest: neue Gefangene werden "eingeliefert", die Zahl der Aliens bleibt begrenzt,
 * und in Abständen greift das Nest die nächste Stadt an.
 */
public final class HiveDirector {
    private HiveDirector() {}

    public static void tick(ServerWorld world) {
        if (world.getTime() % 100 != 0) return;
        WorldState state = WorldState.get(world);
        for (WorldState.City c : state.all()) {
            if (!c.hiveBuilt || c.stage != 2) continue;
            boolean playerNearHive = false, playerNearCity = false;
            for (ServerPlayerEntity p : world.getPlayers()) {
                if (p.squaredDistanceTo(c.hiveX, c.hiveY, c.hiveZ) < 200 * 200) playerNearHive = true;
                if (p.squaredDistanceTo(c.centerX(), p.getY(), c.centerZ()) < 260 * 260) playerNearCity = true;
            }
            HiveBuilder.Layout l = HiveBuilder.layout(c, world.getSeed());
            if (playerNearHive) maintain(world, c, l);
            if (playerNearCity) {
                if (c.nextAttack == 0) { c.nextAttack = world.getTime() + 6000; state.markDirty(); }
                else if (world.getTime() >= c.nextAttack) {
                    attack(world, c, l);
                    c.nextAttack = world.getTime() + 36000 + world.random.nextInt(36000);
                    state.markDirty();
                }
            }
        }
    }

    /** Neue Gefangene, wenn zu wenige übrig sind. */
    private static void maintain(ServerWorld world, WorldState.City c, HiveBuilder.Layout l) {
        Box box = new Box(l.cocoons).expand(12);
        int cocoons = world.getEntitiesByClass(CocoonEntity.class, box, e -> true).size();
        if (cocoons < 4 && world.random.nextInt(6) == 0) {
            double a = world.random.nextDouble() * Math.PI * 2;
            int x = l.cocoons.getX() + (int) Math.round(Math.cos(a) * 7.5), z = l.cocoons.getZ() + (int) Math.round(Math.sin(a) * 7.5);
            int y = HiveBuilder.floor(new Placer(world), new BlockPos(x, l.cocoons.getY(), z));
            CocoonEntity cocoon = ModEntities.COCOON.create(world);
            if (cocoon != null) {
                float yaw = (float) Math.toDegrees(Math.atan2(l.cocoons.getZ() - z, l.cocoons.getX() - x)) - 90f;
                cocoon.refreshPositionAndAngles(x + 0.5, y + 0.6, z + 0.5, yaw, 0);
                cocoon.setBodyYaw(yaw); cocoon.setHeadYaw(yaw);
                cocoon.setVariant(world.random.nextInt(10));
                world.spawnEntity(cocoon);
            }
        }
    }

    /** Angriffswelle auf die Stadt: Aliens strömen aus dem Ausgang. */
    public static void attack(ServerWorld world, WorldState.City c, HiveBuilder.Layout l) {
        BlockPos exit = l.exit1;
        int y = world.getChunk(exit.getX() >> 4, exit.getZ() >> 4).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, exit.getX() & 15, exit.getZ() & 15) + 1;
        BlockPos target = new BlockPos(c.centerX(), c.y, c.centerZ());
        int stalkers = 4 + world.random.nextInt(4);
        for (int i = 0; i < stalkers + 3; i++) {
            var type = i < stalkers ? ModEntities.STALKER : ModEntities.CLINGER;
            var mob = type.create(world);
            if (mob == null) continue;
            mob.refreshPositionAndAngles(exit.getX() + world.random.nextInt(5) - 2 + 0.5, y, exit.getZ() + world.random.nextInt(5) - 2 + 0.5, 0, 0);
            mob.initialize(world, world.getLocalDifficulty(exit), SpawnReason.EVENT, null);
            mob.setPersistent();
            if (mob instanceof StalkerEntity s) s.setRaidTarget(target);
            if (mob instanceof ClingerEntity cl) cl.setRaidTarget(target);
            world.spawnEntity(mob);
        }
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(c.centerX(), p.getY(), c.centerZ()) > 400 * 400) continue;
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.hiveinvasion.alarm").formatted(Formatting.DARK_RED, Formatting.BOLD)));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("title.hiveinvasion.attack", c.name).formatted(Formatting.RED)));
            world.playSound(null, p.getBlockPos(), SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 64f, 0.8f);
        }
    }
}
