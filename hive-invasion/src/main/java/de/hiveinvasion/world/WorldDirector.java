package de.hiveinvasion.world;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.human.CivilianEntity;
import de.hiveinvasion.human.SoldierEntity;
import de.hiveinvasion.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Plant und baut die Städte (mit Stützpunkt und Alien-Nest) in einem Raster aus Regionen.
 * Kommt ein Spieler in die Nähe einer Stadt, wird sie Stück für Stück gebaut und dann bevölkert.
 */
public final class WorldDirector {
    public static final int REGION = 640;
    private static final Deque<CityBuilder.Job> QUEUE = new ArrayDeque<>();
    private static WorldState.City building;

    private WorldDirector() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            tickBuild(world);
            if (world.getTime() % 40 == 0) checkPlayers(world);
            de.hiveinvasion.hive.HiveDirector.tick(world);
        });
    }

    /** Wo liegt die Stadt in Region (rx, rz)? (deterministisch aus dem Weltseed) */
    public static WorldState.City planCity(ServerWorld world, int rx, int rz) {
        Random r = Random.create(world.getSeed() ^ (rx * 341873128712L + rz * 132897987541L));
        int x0, z0;
        if (rx == 0 && rz == 0) { x0 = 150; z0 = -CityBuilder.SIZE / 2; }
        else {
            x0 = rx * REGION + 40 + r.nextInt(REGION - CityBuilder.SIZE - BaseBuilder.W - 100);
            z0 = rz * REGION + 40 + r.nextInt(REGION - CityBuilder.SIZE - 100);
        }
        WorldState.City c = new WorldState.City(rx, rz, x0, z0);
        c.name = Names.city(r);
        double a = r.nextDouble() * Math.PI * 2;
        c.hiveX = c.centerX() + (int) (Math.cos(a) * 170);
        c.hiveZ = c.centerZ() + (int) (Math.sin(a) * 170);
        return c;
    }

    private static void checkPlayers(ServerWorld world) {
        WorldState state = WorldState.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            int prx = Math.floorDiv(player.getBlockX(), REGION), prz = Math.floorDiv(player.getBlockZ(), REGION);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                WorldState.City c = state.city(prx + dx, prz + dz);
                if (c == null) { c = planCity(world, prx + dx, prz + dz); state.put(c); }
                if (c.stage == 0 && building == null
                        && player.squaredDistanceTo(c.centerX(), player.getY(), c.centerZ()) < 360 * 360) startBuild(world, state, c);
            }
        }
    }

    private static void startBuild(ServerWorld world, WorldState state, WorldState.City c) {
        building = c;
        c.stage = 1;
        state.markDirty();
        Placer probe = new Placer(world);
        c.y = CityBuilder.groundY(probe, c.centerX(), c.centerZ());
        List<CityBuilder.Job> jobs = CityBuilder.plan(c.x0, c.z0, c.y, world.getSeed() ^ c.x0 * 31L ^ c.z0);
        int bx = c.baseX(), bz = c.baseZ();
        BaseBuilder.plan(jobs, bx, bz, c.y, c.x0 + CityBuilder.SIZE);
        de.hiveinvasion.hive.HiveBuilder.plan(jobs, c, world.getSeed());
        jobs.add(p -> populate(p.world(), c));
        QUEUE.addAll(jobs);
        HiveInvasion.LOGGER.info("Baue Stadt {} bei {} {} ({} Schritte)", c.name, c.centerX(), c.centerZ(), jobs.size());
    }

    private static void tickBuild(ServerWorld world) {
        if (QUEUE.isEmpty()) return;
        Placer p = new Placer(world);
        long until = System.nanoTime() + 18_000_000L;   // höchstens ~18 ms pro Tick
        while (!QUEUE.isEmpty() && System.nanoTime() < until) QUEUE.poll().run(p);
    }

    private static void populate(ServerWorld world, WorldState.City c) {
        Random r = world.random;
        // Zivilisten auf den Gehwegen
        for (int i = 0; i < 30; i++) {
            int gx = c.x0 + r.nextInt(CityBuilder.N + 1) * CityBuilder.CELL;
            int gz = c.z0 + r.nextInt(CityBuilder.SIZE);
            if (r.nextBoolean()) { int t = gx - c.x0; gx = c.x0 + r.nextInt(CityBuilder.SIZE); gz = c.z0 + t; }
            CivilianEntity civ = ModEntities.CIVILIAN.create(world);
            if (civ == null) continue;
            civ.refreshPositionAndAngles(gx + 0.5, c.y, gz + 0.5, r.nextFloat() * 360, 0);
            civ.setHome(new BlockPos(gx, c.y, gz));
            civ.initialize(world, world.getLocalDifficulty(civ.getBlockPos()), SpawnReason.STRUCTURE, null);
            civ.setPersistent();
            world.spawnEntity(civ);
        }
        // Soldaten im Stützpunkt und zwei Wachen am Tor
        BlockPos base = new BlockPos(c.baseX() + BaseBuilder.W / 2, c.y, BaseBuilder.gateZ(c.baseZ()));
        for (int i = 0; i < 12; i++) {
            SoldierEntity s = ModEntities.SOLDIER.create(world);
            if (s == null) continue;
            boolean guard = i < 2;
            double x = guard ? c.baseX() - 2 : base.getX() - 10 + r.nextInt(20);
            double z = guard ? BaseBuilder.gateZ(c.baseZ()) + (i == 0 ? -3.5 : 3.5) : base.getZ() - 8 + r.nextInt(16);
            s.refreshPositionAndAngles(x + 0.5, c.y, z + 0.5, guard ? 90 : r.nextFloat() * 360, 0);
            s.setHome(guard ? BlockPos.ofFloored(x, c.y, z) : base);
            s.setGuard(guard);
            s.initialize(world, world.getLocalDifficulty(s.getBlockPos()), SpawnReason.STRUCTURE, null);
            s.setPersistent();
            world.spawnEntity(s);
        }
        c.stage = 2;
        WorldState.get(world).markDirty();
        building = null;
        for (ServerPlayerEntity pl : world.getPlayers()) {
            if (pl.squaredDistanceTo(c.centerX(), pl.getY(), c.centerZ()) < 500 * 500)
                pl.sendMessage(Text.translatable("message.hiveinvasion.city_found", c.name).formatted(Formatting.GOLD), false);
        }
        HiveInvasion.LOGGER.info("Stadt {} fertig", c.name);
    }
}
