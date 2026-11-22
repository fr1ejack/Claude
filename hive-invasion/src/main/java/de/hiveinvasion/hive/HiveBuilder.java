package de.hiveinvasion.hive;

import de.hiveinvasion.entity.StalkerEntity;
import de.hiveinvasion.registry.ModBlocks;
import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.world.CityBuilder;
import de.hiveinvasion.world.Placer;
import de.hiveinvasion.world.WorldState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.List;

/**
 * Unterirdisches Alien-Nest: Königinnenkammer in der Mitte, Eierkammer, Kokonkammer,
 * zwei Ruhekammern, organische Harztunnel und 1–2 Ausgänge an die Oberfläche.
 */
public final class HiveBuilder {
    private HiveBuilder() {}

    /** Kammerpositionen eines Nests (deterministisch aus Stadt + Seed). */
    public static final class Layout {
        public BlockPos queen, eggs, cocoons, lounge1, lounge2, exit1, exit2;
    }

    public static Layout layout(WorldState.City c, long seed) {
        Random r = Random.create(seed ^ c.hiveX * 73428767L ^ c.hiveZ);
        double a = r.nextDouble() * Math.PI * 2;
        int y = c.hiveY;
        Layout l = new Layout();
        l.queen = new BlockPos(c.hiveX, y, c.hiveZ);
        l.eggs = around(l.queen, a, 24, -1);
        l.cocoons = around(l.queen, a + 2.1, 25, 1);
        l.lounge1 = around(l.queen, a + 4.2, 22, 0);
        l.lounge2 = around(l.lounge1, a + 3.6, 18, 2);
        // Ausgänge Richtung Stadt
        // Ausgänge immer außerhalb der Stadt (140 Blöcke vom Zentrum, Richtung Nest)
        double fromCity = Math.atan2(c.hiveZ - c.centerZ(), c.hiveX - c.centerX());
        l.exit1 = outside(c, fromCity + 0.15, y);
        l.exit2 = outside(c, fromCity - 0.3, y);
        return l;
    }

    /** Punkt in Richtung angle, sicher außerhalb des (quadratischen) Stadtgebiets samt Böschung. */
    private static BlockPos outside(WorldState.City c, double angle, int y) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double dist = (CityBuilder.SIZE / 2.0 + 26) / Math.max(Math.abs(cos), Math.abs(sin));
        return new BlockPos(c.centerX() + (int) (cos * dist), y, c.centerZ() + (int) (sin * dist));
    }

    private static BlockPos around(BlockPos p, double angle, double dist, int dy) {
        return p.add((int) (Math.cos(angle) * dist), dy, (int) (Math.sin(angle) * dist));
    }

    public static void plan(List<CityBuilder.Job> jobs, WorldState.City c, long seed) {
        jobs.add(p -> {
            int surface = p.world().getChunk(c.hiveX >> 4, c.hiveZ >> 4)
                    .sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, c.hiveX & 15, c.hiveZ & 15);
            if (surface <= p.world().getBottomY()) surface = c.y;
            c.hiveY = Math.max(-40, Math.min(surface, c.y) - 30);
        });
        jobs.add(p -> {
            Layout l = layout(c, seed);
            Random r = Random.create(seed ^ 991);
            chamber(p, l.queen, 11, 7, r);
            chamber(p, l.eggs, 8, 5, r);
            chamber(p, l.cocoons, 9, 6, r);
            chamber(p, l.lounge1, 7, 5, r);
            chamber(p, l.lounge2, 6, 4, r);
        });
        jobs.add(p -> {
            Layout l = layout(c, seed);
            Random r = Random.create(seed ^ 1337);
            tunnel(p, l.queen, l.eggs, 2.6, r);
            tunnel(p, l.queen, l.cocoons, 2.6, r);
            tunnel(p, l.queen, l.lounge1, 2.8, r);
            tunnel(p, l.lounge1, l.lounge2, 2.4, r);
            tunnel(p, l.eggs, l.cocoons, 2.2, r);
        });
        jobs.add(p -> {
            Layout l = layout(c, seed);
            Random r = Random.create(seed ^ 4242);
            exit(p, l.lounge1, l.exit1, r);
            if (r.nextBoolean()) exit(p, l.eggs, l.exit2, r);
        });
        jobs.add(p -> furnish(p, c, layout(c, seed)));
        jobs.add(p -> { populate(p, c, layout(c, seed)); c.hiveBuilt = true; });
    }

    static final BlockState AIR = Blocks.AIR.getDefaultState();

    static BlockState wall(Random r) {
        float f = r.nextFloat();
        if (f < 0.04f) return ModBlocks.HIVE_GLOW.getDefaultState();
        if (f < 0.30f) return ModBlocks.HIVE_RIBS.getDefaultState();
        return ModBlocks.HIVE_RESIN.getDefaultState();
    }

    /** Ellipsoid-Kammer: innen Luft, Wand aus Harz, unregelmäßig. */
    static void chamber(Placer p, BlockPos c, int rh, int rv, Random r) {
        int pad = 2;
        for (int x = -rh - pad; x <= rh + pad; x++) for (int y = -rv - pad; y <= rv + pad; y++) for (int z = -rh - pad; z <= rh + pad; z++) {
            double wobble = 1 + 0.12 * Math.sin(x * 0.7 + z * 0.4) + 0.08 * Math.cos(y * 1.1 + x * 0.3);
            double d = Math.sqrt((x * x + z * z) / (double) (rh * rh) + (y * y) / (double) (rv * rv)) / wobble;
            if (d < 1) {
                if (y < -rv * 0.7) p.set(c.getX() + x, c.getY() + y, c.getZ() + z, ModBlocks.HIVE_RESIN.getDefaultState()); // flacher Boden
                else p.set(c.getX() + x, c.getY() + y, c.getZ() + z, AIR);
            } else if (d < 1 + 2.2 / rh) {
                p.set(c.getX() + x, c.getY() + y, c.getZ() + z, wall(r));
            }
        }
        // Säulen / Stalaktiten aus Harz
        for (int i = 0; i < rh; i++) {
            int x = c.getX() + r.nextInt(rh * 2) - rh, z = c.getZ() + r.nextInt(rh * 2) - rh;
            int len = 1 + r.nextInt(rv);
            for (int k = 0; k < len; k++) {
                int yy = c.getY() + rv - 1 - k;
                if (p.get(x, yy, z).isAir()) p.set(x, yy, z, ModBlocks.HIVE_RIBS.getDefaultState());
            }
        }
    }

    /** Organisch gebogener Tunnel zwischen zwei Punkten. */
    static void tunnel(Placer p, BlockPos a, BlockPos b, double radius, Random r) {
        Vec3d A = Vec3d.ofCenter(a), B = Vec3d.ofCenter(b);
        Vec3d mid = A.add(B).multiply(0.5).add(r.nextGaussian() * 4, r.nextGaussian() * 1.5, r.nextGaussian() * 4);
        int steps = (int) (A.distanceTo(B) * 1.5);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d q = A.multiply((1 - t) * (1 - t)).add(mid.multiply(2 * (1 - t) * t)).add(B.multiply(t * t));
            double rad = radius * (0.9 + 0.2 * Math.sin(t * 9));
            sphere(p, q, rad, r);
        }
    }

    static void sphere(Placer p, Vec3d q, double rad, Random r) {
        int R = (int) Math.ceil(rad + 1.6);
        for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + y * y + z * z);
            int bx = MathHelper.floor(q.x) + x, by = MathHelper.floor(q.y) + y, bz = MathHelper.floor(q.z) + z;
            if (d < rad) {
                if (y < -rad * 0.6) p.set(bx, by, bz, ModBlocks.HIVE_RESIN.getDefaultState());
                else p.set(bx, by, bz, AIR);
            } else if (d < rad + 1.5) {
                BlockState s = p.get(bx, by, bz);
                if (!s.isAir() && !s.isOf(ModBlocks.HIVE_RESIN) && !s.isOf(ModBlocks.HIVE_RIBS) && !s.isOf(ModBlocks.HIVE_GLOW)) p.set(bx, by, bz, wall(r));
            }
        }
    }

    /** Schräger Tunnel nach oben mit Harzhügel am Ausgang. */
    static void exit(Placer p, BlockPos from, BlockPos to, Random r) {
        var w = p.world();
        int surface = w.getChunk(to.getX() >> 4, to.getZ() >> 4).sampleHeightmap(Heightmap.Type.WORLD_SURFACE, to.getX() & 15, to.getZ() & 15);
        BlockPos top = new BlockPos(to.getX(), surface + 1, to.getZ());
        tunnel(p, from, top, 2.3, r);
        // Harzhügel um den Ausgang
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > 2.6 && d < 5 && r.nextFloat() < 0.8f) {
                int h = (int) (3 - (d - 2.6)) + r.nextInt(2);
                for (int k = 0; k < h; k++) p.set(top.getX() + x, surface + k, top.getZ() + z, wall(r));
            }
        }
    }

    static void furnish(Placer p, WorldState.City c, Layout l) {
        Random r = Random.create(c.hiveX * 31L + c.hiveZ);
        // Eierkammer voller Brutkapseln
        floorScatter(p, l.eggs, 6, 4, 14, ModBlocks.BROOD_POD.getDefaultState(), r);
        // Königin: einige Kapseln rund um sie
        floorScatter(p, l.queen, 9, 6, 6, ModBlocks.BROOD_POD.getDefaultState(), r);
    }

    static void floorScatter(Placer p, BlockPos c, int radius, int rv, int count, BlockState s, Random r) {
        for (int i = 0; i < count * 4 && count > 0; i++) {
            int x = c.getX() + r.nextInt(radius * 2 + 1) - radius, z = c.getZ() + r.nextInt(radius * 2 + 1) - radius;
            for (int y = c.getY(); y > c.getY() - rv - 1; y--) {
                if (p.get(x, y, z).isAir() && !p.get(x, y - 1, z).isAir()) {
                    p.set(x, y, z, s);
                    count--;
                    break;
                }
            }
        }
    }

    static void populate(Placer p, WorldState.City c, Layout l) {
        var world = p.world();
        Random r = world.random;
        // Königin
        QueenEntity queen = ModEntities.QUEEN.create(world);
        if (queen != null) {
            queen.refreshPositionAndAngles(l.queen.getX() + 0.5, floor(p, l.queen), l.queen.getZ() + 0.5, r.nextFloat() * 360, 0);
            queen.setHive(l);
            queen.initialize(world, world.getLocalDifficulty(l.queen), SpawnReason.STRUCTURE, null);
            world.spawnEntity(queen);
        }
        // Gefangene in der Kokonkammer: an der Wand festgeklebt
        for (int i = 0; i < 9; i++) {
            double a = i / 9.0 * Math.PI * 2;
            int x = l.cocoons.getX() + (int) Math.round(Math.cos(a) * 7.5), z = l.cocoons.getZ() + (int) Math.round(Math.sin(a) * 7.5);
            int y = floor(p, new BlockPos(x, l.cocoons.getY(), z));
            CocoonEntity cocoon = ModEntities.COCOON.create(world);
            if (cocoon == null) continue;
            float yaw = (float) Math.toDegrees(Math.atan2(l.cocoons.getZ() - z, l.cocoons.getX() - x)) - 90f;
            cocoon.refreshPositionAndAngles(x + 0.5, y + 0.6, z + 0.5, yaw, 0);
            cocoon.setBodyYaw(yaw); cocoon.setHeadYaw(yaw);
            cocoon.setVariant(r.nextInt(10));
            world.spawnEntity(cocoon);
        }
        // Stalker in den Ruhekammern
        for (int i = 0; i < 6; i++) {
            BlockPos at = i < 4 ? l.lounge1 : l.lounge2;
            StalkerEntity s = ModEntities.STALKER.create(world);
            if (s == null) continue;
            s.refreshPositionAndAngles(at.getX() + r.nextInt(5) - 2 + 0.5, floor(p, at), at.getZ() + r.nextInt(5) - 2 + 0.5, r.nextFloat() * 360, 0);
            s.setHive(l);
            s.setPersistent();
            s.initialize(world, world.getLocalDifficulty(at), SpawnReason.STRUCTURE, null);
            world.spawnEntity(s);
        }
    }

    /** Erste freie Höhe über dem Kammerboden. */
    public static int floor(Placer p, BlockPos c) {
        for (int y = c.getY() - 8; y < c.getY() + 4; y++) {
            if (!p.get(c.getX(), y - 1, c.getZ()).isAir() && p.get(c.getX(), y, c.getZ()).isAir() && p.get(c.getX(), y + 1, c.getZ()).isAir()) return y;
        }
        return c.getY();
    }
}
