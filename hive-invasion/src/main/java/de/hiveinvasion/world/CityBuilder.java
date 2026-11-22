package de.hiveinvasion.world;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Baut eine Stadt prozedural: Gelände einebnen, Straßenraster, Hochhäuser in der Mitte,
 * Wohnblöcke drumherum, Einfamilienhäuser und Parks außen, Autos, Laternen.
 * Liefert eine Liste von Arbeitsschritten, die nacheinander (einer pro Tick) ausgeführt werden.
 */
public final class CityBuilder {
    public static final int CELL = 26, ROAD = 9, LOT = CELL - ROAD, N = 8;
    public static final int SIZE = N * CELL + ROAD;

    private static final BlockState AIR = Blocks.AIR.getDefaultState(),
            ASPHALT = Blocks.GRAY_CONCRETE.getDefaultState(), LINE_Y = Blocks.YELLOW_CONCRETE.getDefaultState(),
            LINE_W = Blocks.WHITE_CONCRETE.getDefaultState(), WALK = Blocks.SMOOTH_STONE.getDefaultState(),
            CURB = Blocks.POLISHED_ANDESITE.getDefaultState(), GRASS = Blocks.GRASS_BLOCK.getDefaultState(),
            STONE = Blocks.STONE.getDefaultState(), DIRT = Blocks.DIRT.getDefaultState();

    public interface Job { void run(Placer p); }

    private CityBuilder() {}

    /** Bodenhöhe der Stadt an ihrer Mitte. */
    public static int groundY(Placer p, int cx, int cz) {
        int y = p.world().getChunk(cx >> 4, cz >> 4).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx & 15, cz & 15) + 1;
        return Math.max(64, Math.min(110, y));
    }

    public static List<Job> plan(int x0, int z0, int y, long seed) {
        List<Job> jobs = new ArrayList<>();
        // 1. Gelände: einebnen, Fundament
        for (int tx = 0; tx < SIZE; tx += 16)
            for (int tz = 0; tz < SIZE; tz += 16) {
                final int ax = x0 + tx, az = z0 + tz;
                jobs.add(p -> terrain(p, ax, az, Math.min(16, x0 + SIZE - ax), Math.min(16, z0 + SIZE - az), y));
            }
        for (int k = 0; k < 4; k++) {
            final int side = k;
            jobs.add(p -> {
                int x1 = x0 + SIZE - 1, z1 = z0 + SIZE - 1, w = 16;
                switch (side) {
                    case 0 -> embankment(p, x0, z0, x1, z0, y, w);
                    case 1 -> embankment(p, x0, z1, x1, z1, y, w);
                    case 2 -> embankment(p, x0, z0, x0, z1, y, w);
                    default -> embankment(p, x1, z0, x1, z1, y, w);
                }
            });
        }
        // 2. Straßen
        for (int i = 0; i <= N; i++) {
            final int off = i * CELL;
            jobs.add(p -> road(p, x0 + off, z0, true, y, SIZE));
            jobs.add(p -> road(p, x0, z0 + off, false, y, SIZE));
        }
        for (int i = 0; i <= N; i++) for (int j = 0; j <= N; j++) {
            final int ix = x0 + i * CELL, iz = z0 + j * CELL;
            jobs.add(p -> intersection(p, ix, iz, y));
        }
        // 3. Grundstücke
        Random r = Random.create(seed);
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
            final int lx = x0 + i * CELL + ROAD, lz = z0 + j * CELL + ROAD;
            double d = Math.max(Math.abs(i - (N - 1) / 2.0), Math.abs(j - (N - 1) / 2.0));
            final long ls = r.nextLong();
            if (d < 1.6) jobs.add(p -> skyscraper(p, lx, lz, y, Random.create(ls)));
            else if (d < 2.6) jobs.add(p -> apartment(p, lx, lz, y, Random.create(ls)));
            else if (r.nextFloat() < 0.18f) jobs.add(p -> park(p, lx, lz, y, Random.create(ls)));
            else jobs.add(p -> houses(p, lx, lz, y, Random.create(ls)));
            jobs.add(p -> streetlife(p, lx, lz, y, Random.create(ls ^ 77)));
        }
        return jobs;
    }

    // ------------------------------------------------------------------ Gelände
    static void terrain(Placer p, int x0, int z0, int w, int d, int y) {
        var world = p.world();
        int bottom = Math.max(world.getBottomY() + 1, y - 64);
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) {
            int top = world.getChunk(x >> 4, z >> 4).sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15);
            for (int yy = Math.min(top + 1, y + 45); yy >= y; yy--) {
                if (!p.get(x, yy, z).isAir()) p.set(x, yy, z, AIR);
            }
            // Fundament bis zum natürlichen Boden auffüllen
            for (int yy = y - 1; yy >= bottom; yy--) {
                BlockState s = p.get(x, yy, z);
                boolean soft = s.isAir() || !s.getFluidState().isEmpty() || s.isReplaceable()
                        || s.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) || s.isIn(net.minecraft.registry.tag.BlockTags.LOGS);
                if (!soft && yy < y - 3) break;
                p.set(x, yy, z, yy == y - 1 ? GRASS : yy > y - 4 ? DIRT : STONE);
            }
        }
    }

    /** Böschung rund um ein Rechteck: Gelände läuft weich auf die Stadthöhe zu. */
    public static void embankment(Placer p, int x0, int z0, int x1, int z1, int y, int width) {
        var world = p.world();
        for (int x = x0 - width; x <= x1 + width; x++) for (int z = z0 - width; z <= z1 + width; z++) {
            int dx = Math.max(0, Math.max(x0 - x, x - x1)), dz = Math.max(0, Math.max(z0 - z, z - z1));
            int dist = Math.max(dx, dz);
            if (dist == 0) continue;
            int top = world.getChunk(x >> 4, z >> 4).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) - 1;
            int target = y - 1 - (int) Math.round(dist * 0.9 + (dist * dist) * 0.02);
            if (top < target) {
                for (int yy = top + 1; yy <= target; yy++) p.set(x, yy, z, yy == target ? GRASS : DIRT);
            } else if (top > y + 1 && dist < width) {
                int cut = y - 1 + (int) Math.round(dist * 1.2);
                for (int yy = top; yy > cut; yy--) p.set(x, yy, z, AIR);
                if (cut < top) p.set(x, cut, z, GRASS);
            }
        }
    }

    // ------------------------------------------------------------------ Straßen
    static void road(Placer p, int x0, int z0, boolean alongZ, int y, int len) {
        for (int t = 0; t < len; t++) for (int k = 0; k < ROAD; k++) {
            int x = alongZ ? x0 + k : x0 + t, z = alongZ ? z0 + t : z0 + k;
            BlockState s = k < 2 || k > 6 ? WALK : ASPHALT;
            if (k == 1 || k == 7) s = WALK;
            if (k == 2 || k == 6) s = ASPHALT;
            if (k == 4 && (t / 3) % 2 == 0) s = LINE_Y;
            p.set(x, y - 1, z, s);
            if (k == 1 || k == 7) p.set(x, y - 1, z, CURB);
        }
    }

    static void intersection(Placer p, int x0, int z0, int y) {
        for (int a = 0; a < ROAD; a++) for (int b = 0; b < ROAD; b++) {
            boolean walk = (a < 2 || a > 6) && (b < 2 || b > 6);
            p.set(x0 + a, y - 1, z0 + b, walk ? WALK : ASPHALT);
        }
        // Zebrastreifen an allen vier Seiten
        for (int k = 2; k <= 6; k++) for (int s = 0; s < 2; s++) {
            if (k % 2 == 0) {
                p.set(x0 + k, y - 1, z0 - 1 - s, LINE_W); p.set(x0 + k, y - 1, z0 + ROAD + s, LINE_W);
                p.set(x0 - 1 - s, y - 1, z0 + k, LINE_W); p.set(x0 + ROAD + s, y - 1, z0 + k, LINE_W);
            }
        }
        // Ampel-Laterne an der Ecke
        lamp(p, x0 + 1, y, z0 + 1);
    }

    static void lamp(Placer p, int x, int y, int z) {
        for (int h = 0; h < 5; h++) p.set(x, y + h, z, Blocks.ANDESITE_WALL.getDefaultState());
        p.set(x, y + 5, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        p.set(x, y + 4, z, Blocks.ANDESITE_WALL.getDefaultState());
        p.set(x + 1, y + 5, z, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        p.set(x + 1, y + 4, z, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    // ------------------------------------------------------------------ Gebäude
    static final BlockState[] GLASS = {Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.GRAY_STAINED_GLASS.getDefaultState(),
            Blocks.CYAN_STAINED_GLASS.getDefaultState(), Blocks.BLACK_STAINED_GLASS.getDefaultState()};
    static final BlockState[] FRAME = {Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState(),
            Blocks.GRAY_CONCRETE.getDefaultState(), Blocks.SMOOTH_QUARTZ.getDefaultState(), Blocks.POLISHED_DEEPSLATE.getDefaultState()};

    static void skyscraper(Placer p, int x0, int z0, int y, Random r) {
        int w = 15, h = 32 + r.nextInt(36);
        h -= h % 4;
        BlockState glass = GLASS[r.nextInt(GLASS.length)], frame = FRAME[r.nextInt(FRAME.length)];
        BlockState floor = Blocks.SMOOTH_STONE.getDefaultState();
        int ax = x0 + 1, az = z0 + 1, bx = ax + w - 1, bz = az + w - 1;
        // Plaza
        p.box(x0, y - 1, z0, x0 + LOT - 1, y - 1, z0 + LOT - 1, Blocks.POLISHED_ANDESITE.getDefaultState());
        for (int yy = 0; yy < h; yy++) {
            boolean band = yy % 4 == 0;
            for (int x = ax; x <= bx; x++) for (int z = az; z <= bz; z++) {
                boolean edge = x == ax || x == bx || z == az || z == bz;
                if (!edge) continue;
                boolean corner = (x == ax || x == bx) && (z == az || z == bz);
                boolean pillar = (x - ax) % 4 == 0 && (z == az || z == bz) || (z - az) % 4 == 0 && (x == ax || x == bx);
                p.set(x, y + yy, z, band || corner || pillar ? frame : glass);
            }
            if (band) {
                p.box(ax + 1, y + yy, az + 1, bx - 1, y + yy, bz - 1, floor);
                if (yy > 0 && r.nextFloat() < 0.65f) { // Deckenlicht in manchen Etagen
                    p.set(ax + 4, y + yy, az + 4, Blocks.SEA_LANTERN.getDefaultState());
                    p.set(bx - 4, y + yy, bz - 4, Blocks.SEA_LANTERN.getDefaultState());
                }
            }
        }
        // Eingang
        int mid = ax + w / 2;
        p.air(mid - 1, y, az, mid + 1, y + 2, az);
        p.box(mid - 2, y + 3, az - 1, mid + 2, y + 3, az - 1, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        p.set(mid, y + 3, az + 2, Blocks.SEA_LANTERN.getDefaultState());
        // Dach: Brüstung, Klimaanlagen, Antenne, evtl. Hubschrauberplatz
        int top = y + h;
        p.box(ax, top, az, bx, top, bz, frame);
        p.walls(ax, top + 1, az, bx, top + 1, bz, frame);
        if (r.nextBoolean()) {
            for (int k = 0; k < 3; k++) {
                int ux = ax + 2 + r.nextInt(w - 5), uz = az + 2 + r.nextInt(w - 5);
                p.box(ux, top + 1, uz, ux + 1, top + 2, uz + 1, Blocks.IRON_BLOCK.getDefaultState());
                p.set(ux, top + 3, uz, Blocks.IRON_TRAPDOOR.getDefaultState());
            }
            for (int a = 0; a < 8 + r.nextInt(8); a++) p.set(ax + w / 2, top + 1 + a, az + w / 2, Blocks.LIGHTNING_ROD.getDefaultState());
            p.set(ax + w / 2, top + 17, az + w / 2, Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true));
        } else {
            for (int x = ax + 3; x <= bx - 3; x++) for (int z = az + 3; z <= bz - 3; z++)
                p.set(x, top, z, Blocks.GRAY_CONCRETE.getDefaultState());
            int c = ax + w / 2, d = az + w / 2;
            for (int k = -2; k <= 2; k++) { p.set(c - 2, top, d + k, LINE_Y); p.set(c + 2, top, d + k, LINE_Y); }
            p.set(c - 1, top, d, LINE_Y); p.set(c, top, d, LINE_Y); p.set(c + 1, top, d, LINE_Y);
            p.set(ax + 1, top + 1, az + 1, Blocks.GLOWSTONE.getDefaultState()); p.set(bx - 1, top + 1, bz - 1, Blocks.GLOWSTONE.getDefaultState());
        }
    }

    static void apartment(Placer p, int x0, int z0, int y, Random r) {
        BlockState[] walls = {Blocks.BRICKS.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(),
                Blocks.LIGHT_GRAY_TERRACOTTA.getDefaultState(), Blocks.TERRACOTTA.getDefaultState(), Blocks.SANDSTONE.getDefaultState()};
        BlockState wall = walls[r.nextInt(walls.length)], trim = Blocks.SMOOTH_STONE.getDefaultState();
        int h = 14 + r.nextInt(14);
        h -= h % 4;
        int ax = x0 + 1, az = z0 + 2, bx = x0 + LOT - 2, bz = z0 + LOT - 3;
        p.box(x0, y - 1, z0, x0 + LOT - 1, y - 1, z0 + LOT - 1, GRASS);
        for (int yy = 0; yy < h; yy++) {
            boolean band = yy % 4 == 0;
            for (int x = ax; x <= bx; x++) for (int z = az; z <= bz; z++) {
                boolean edge = x == ax || x == bx || z == az || z == bz;
                if (!edge) continue;
                boolean window = !band && yy % 4 != 3 && ((x - ax) % 3 == 1 || (z - az) % 3 == 1)
                        && !((x == ax || x == bx) && (z == az || z == bz));
                p.set(x, y + yy, z, band ? trim : window ? Blocks.GLASS_PANE.getDefaultState() : wall);
            }
            if (band) {
                p.box(ax + 1, y + yy, az + 1, bx - 1, y + yy, bz - 1, Blocks.OAK_PLANKS.getDefaultState());
                if (yy > 0) p.set(ax + 3, y + yy, az + 3, Blocks.GLOWSTONE.getDefaultState());
                // Balkone vorn
                if (yy > 0) for (int x = ax + 2; x <= bx - 2; x += 4) {
                    p.set(x, y + yy, az - 1, trim); p.set(x + 1, y + yy, az - 1, trim);
                    p.set(x, y + yy + 1, az - 2 + 1, Blocks.IRON_BARS.getDefaultState());
                }
            }
        }
        int mid = (ax + bx) / 2;
        p.air(mid, y, az, mid, y + 1, az);
        p.set(mid, y, az, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH));
        p.set(mid, y + 1, az, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        p.box(ax, y + h, az, bx, y + h, bz, Blocks.GRAY_CONCRETE.getDefaultState());
        p.walls(ax, y + h + 1, az, bx, y + h + 1, bz, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        p.box(bx - 3, y + h + 1, bz - 3, bx - 2, y + h + 3, bz - 2, Blocks.IRON_BLOCK.getDefaultState());
    }

    static void houses(Placer p, int x0, int z0, int y, Random r) {
        p.box(x0, y - 1, z0, x0 + LOT - 1, y - 1, z0 + LOT - 1, GRASS);
        house(p, x0 + 1, z0 + 1, y, r);
        house(p, x0 + 9, z0 + 1, y, r);
        // Gartenzaun hinten
        for (int x = x0; x < x0 + LOT; x++) p.set(x, y, z0 + LOT - 1, Blocks.SPRUCE_FENCE.getDefaultState());
        if (r.nextBoolean()) tree(p, x0 + 4, y, z0 + 12, r);
        if (r.nextBoolean()) tree(p, x0 + 12, y, z0 + 13, r);
    }

    static void house(Placer p, int x0, int z0, int y, Random r) {
        BlockState[] walls = {Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.BRICKS.getDefaultState(),
                Blocks.STRIPPED_BIRCH_LOG.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState()};
        BlockState[] roofs = {Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.BRICK_STAIRS.getDefaultState(),
                Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState(), Blocks.SPRUCE_STAIRS.getDefaultState()};
        BlockState wall = walls[r.nextInt(walls.length)], roof = roofs[r.nextInt(roofs.length)];
        int w = 7, d = 8, h = 4;
        int bx = x0 + w - 1, bz = z0 + d - 1;
        p.box(x0, y - 1, z0, bx, y - 1, bz, Blocks.SPRUCE_PLANKS.getDefaultState());
        for (int yy = 0; yy < h; yy++) for (int x = x0; x <= bx; x++) for (int z = z0; z <= bz; z++) {
            boolean edge = x == x0 || x == bx || z == z0 || z == bz;
            if (!edge) continue;
            boolean window = yy == 1 && ((x - x0) % 3 == 2 || (z - z0) % 3 == 2) && !((x == x0 || x == bx) && (z == z0 || z == bz));
            p.set(x, y + yy, z, window ? Blocks.GLASS_PANE.getDefaultState() : wall);
        }
        p.set(x0 + 3, y, z0, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH));
        p.set(x0 + 3, y + 1, z0, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        p.set(x0 + 3, y + 2, z0 - 1, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
        // Satteldach (First entlang z)
        for (int k = 0; k <= w / 2; k++) {
            for (int z = z0 - 1; z <= bz + 1; z++) {
                p.set(x0 + k - 1 + 1, y + h + k, z, roof.with(StairsBlock.FACING, Direction.EAST));
                p.set(bx - k, y + h + k, z, roof.with(StairsBlock.FACING, Direction.WEST));
            }
        }
        for (int k = 0; k < w / 2; k++) for (int x = x0 + k + 1; x <= bx - k - 1; x++) {
            p.set(x, y + h + k, z0, wall); p.set(x, y + h + k, bz, wall);
        }
        p.set(x0 + 2, y + h - 1, z0 + 2, Blocks.GLOWSTONE.getDefaultState());
        // Weg zur Straße
        for (int z = z0 - 1; z >= z0 - 2; z--) p.set(x0 + 3, y - 1, z, Blocks.DIRT_PATH.getDefaultState());
    }

    static void park(Placer p, int x0, int z0, int y, Random r) {
        p.box(x0, y - 1, z0, x0 + LOT - 1, y - 1, z0 + LOT - 1, GRASS);
        for (int k = 0; k < LOT; k++) { p.set(x0 + LOT / 2, y - 1, z0 + k, Blocks.GRAVEL.getDefaultState()); p.set(x0 + k, y - 1, z0 + LOT / 2, Blocks.GRAVEL.getDefaultState()); }
        for (int t = 0; t < 5; t++) tree(p, x0 + 2 + r.nextInt(LOT - 4), y, z0 + 2 + r.nextInt(LOT - 4), r);
        p.set(x0 + LOT / 2 - 1, y, z0 + 4, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        p.set(x0 + LOT / 2 + 1, y, z0 + 12, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        p.set(x0 + LOT / 2, y, z0 + LOT / 2, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
        for (int[] f : new int[][]{{2, 2}, {14, 3}, {3, 13}}) p.set(x0 + f[0], y, z0 + f[1], (r.nextBoolean() ? Blocks.POPPY : Blocks.DANDELION).getDefaultState());
    }

    static void tree(Placer p, int x, int y, int z, Random r) {
        int h = 4 + r.nextInt(3);
        BlockState leaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 2; dy++) {
            if (dx * dx + dz * dz + dy * dy * 2 <= 6 && r.nextFloat() < 0.92f) p.set(x + dx, y + h + dy, z + dz, leaves);
        }
        for (int k = 0; k < h; k++) p.set(x, y + k, z, Blocks.OAK_LOG.getDefaultState());
    }

    /** Laternen am Gehweg und geparkte Autos neben dem Grundstück. */
    static void streetlife(Placer p, int lx, int lz, int y, Random r) {
        for (int k = 3; k < LOT; k += 8) lamp(p, lx + k, y, lz - 2);
        if (r.nextFloat() < 0.55f) car(p, lx + 2 + r.nextInt(LOT - 6), y, lz - 4, r);
    }

    static final BlockState[] PAINT = {Blocks.RED_CONCRETE.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(),
            Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
            Blocks.GREEN_CONCRETE.getDefaultState(), Blocks.YELLOW_CONCRETE.getDefaultState()};

    /** Geparktes Auto (4 lang, 2 breit) entlang der x-Achse. */
    static void car(Placer p, int x, int y, int z, Random r) {
        BlockState paint = PAINT[r.nextInt(PAINT.length)];
        BlockState wheel = Blocks.BLACKSTONE_SLAB.getDefaultState();
        for (int a = 0; a < 4; a++) for (int b = 0; b < 2; b++) {
            p.set(x + a, y, z + b, (a == 0 || a == 3) ? paint : paint);
        }
        p.set(x, y, z, wheel.with(SlabBlock.TYPE, SlabType.TOP)); p.set(x + 3, y, z + 1, wheel.with(SlabBlock.TYPE, SlabType.TOP));
        p.set(x, y, z + 1, wheel.with(SlabBlock.TYPE, SlabType.TOP)); p.set(x + 3, y, z, wheel.with(SlabBlock.TYPE, SlabType.TOP));
        p.set(x + 1, y + 1, z, Blocks.GRAY_STAINED_GLASS.getDefaultState()); p.set(x + 1, y + 1, z + 1, Blocks.GRAY_STAINED_GLASS.getDefaultState());
        p.set(x + 2, y + 1, z, paint); p.set(x + 2, y + 1, z + 1, paint);
        p.set(x, y + 1, z, Blocks.AIR.getDefaultState());
        p.set(x + 3, y, z, Blocks.QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST).with(StairsBlock.HALF, BlockHalf.BOTTOM));
    }
}
