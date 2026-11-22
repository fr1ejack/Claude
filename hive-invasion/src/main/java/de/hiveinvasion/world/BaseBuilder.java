package de.hiveinvasion.world;

import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Militärstützpunkt: Betonmauer mit Stacheldraht und Tor (Westseite, zur Stadt),
 * Wachhäuschen, vier Wachtürme, Kaserne, Hauptquartier, Hubschrauberplatz, Sandsäcke, Fahnenmast.
 */
public final class BaseBuilder {
    public static final int W = 56, D = 48;
    private static final BlockState CONCRETE = Blocks.GRAY_CONCRETE.getDefaultState(), DARK = Blocks.POLISHED_DEEPSLATE.getDefaultState(),
            OLIVE = Blocks.GREEN_TERRACOTTA.getDefaultState(), BARS = Blocks.IRON_BARS.getDefaultState(),
            SAND = Blocks.PACKED_MUD.getDefaultState(), Y = Blocks.YELLOW_CONCRETE.getDefaultState(), WHITE = Blocks.WHITE_CONCRETE.getDefaultState();

    private BaseBuilder() {}

    public static int gateZ(int z0) { return z0 + D / 2; }

    public static void plan(List<CityBuilder.Job> jobs, int x0, int z0, int y, int roadFromX) {
        for (int tx = 0; tx < W; tx += 16) for (int tz = 0; tz < D; tz += 16) {
            final int ax = x0 + tx, az = z0 + tz;
            jobs.add(p -> CityBuilder.terrain(p, ax, az, Math.min(16, x0 + W - ax), Math.min(16, z0 + D - az), y));
        }
        jobs.add(p -> CityBuilder.embankment(p, x0, z0, x0 + W - 1, z0 + D - 1, y, 14));
        // Zufahrtsstraße von der Stadt zum Tor
        jobs.add(p -> {
            int gz = gateZ(z0);
            CityBuilder.terrain(p, roadFromX, gz - 4, x0 - roadFromX, 9, y);
            for (int x = roadFromX; x < x0; x++) for (int k = -4; k <= 4; k++) {
                BlockState s = Math.abs(k) >= 3 ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
                if (k == 0 && (x / 3) % 2 == 0) s = Y;
                p.set(x, y - 1, gz + k, s);
            }
        });
        jobs.add(p -> ground(p, x0, z0, y));
        jobs.add(p -> walls(p, x0, z0, y));
        jobs.add(p -> { tower(p, x0, z0, y); tower(p, x0 + W - 4, z0, y); tower(p, x0, z0 + D - 4, y); tower(p, x0 + W - 4, z0 + D - 4, y); });
        jobs.add(p -> building(p, x0 + 6, z0 + 4, 18, 10, 6, y, OLIVE));               // Kaserne
        jobs.add(p -> building(p, x0 + 6, z0 + D - 15, 14, 11, 10, y, CONCRETE));      // Hauptquartier
        jobs.add(p -> helipad(p, x0 + W - 18, z0 + D / 2 - 7, y));
        jobs.add(p -> details(p, x0, z0, y));
    }

    static void ground(Placer p, int x0, int z0, int y) {
        for (int x = x0; x < x0 + W; x++) for (int z = z0; z < z0 + D; z++)
            p.set(x, y - 1, z, ((x + z) % 7 == 0) ? Blocks.GRAVEL.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
        // Weg vom Tor in die Mitte
        int gz = gateZ(z0);
        for (int x = x0; x < x0 + W - 4; x++) for (int k = -2; k <= 2; k++) p.set(x, y - 1, gz + k, CONCRETE);
    }

    static void walls(Placer p, int x0, int z0, int y) {
        int x1 = x0 + W - 1, z1 = z0 + D - 1, gz = gateZ(z0);
        for (int h = 0; h < 6; h++) {
            for (int x = x0; x <= x1; x++) { p.set(x, y + h, z0, CONCRETE); p.set(x, y + h, z1, CONCRETE); }
            for (int z = z0; z <= z1; z++) { p.set(x0, y + h, z, CONCRETE); p.set(x1, y + h, z, CONCRETE); }
        }
        for (int x = x0; x <= x1; x++) { p.set(x, y + 6, z0, BARS); p.set(x, y + 6, z1, BARS); }
        for (int z = z0; z <= z1; z++) { p.set(x0, y + 6, z, BARS); p.set(x1, y + 6, z, BARS); }
        for (int x = x0; x <= x1; x += 2) { p.set(x, y + 5, z0, DARK); p.set(x, y + 5, z1, DARK); }
        // Tor
        p.air(x0, y, gz - 2, x0, y + 6, gz + 2);
        for (int h = 0; h < 8; h++) { p.set(x0, y + h, gz - 3, DARK); p.set(x0, y + h, gz + 3, DARK); }
        p.box(x0, y + 7, gz - 3, x0, y + 7, gz + 3, DARK);
        p.set(x0 - 1, y + 6, gz - 3, Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true));
        p.set(x0 - 1, y + 6, gz + 3, Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true));
        // Schranke (offen) und Wachhäuschen
        for (int h = 0; h < 4; h++) p.set(x0 + 2, y + h, gz - 4, Blocks.YELLOW_TERRACOTTA.getDefaultState());
        p.box(x0 + 2, y, gz + 4, x0 + 5, y + 3, gz + 7, WHITE);
        p.air(x0 + 3, y, gz + 5, x0 + 4, y + 2, gz + 6);
        p.set(x0 + 2, y + 1, gz + 5, Blocks.GLASS_PANE.getDefaultState()); p.set(x0 + 2, y + 1, gz + 6, Blocks.GLASS_PANE.getDefaultState());
        p.set(x0 + 3, y, gz + 4, Blocks.AIR.getDefaultState()); p.set(x0 + 3, y + 1, gz + 4, Blocks.AIR.getDefaultState());
        p.box(x0 + 1, y + 4, gz + 3, x0 + 6, y + 4, gz + 8, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        p.set(x0 + 4, y + 3, gz + 5, Blocks.SEA_LANTERN.getDefaultState());
    }

    static void tower(Placer p, int x, int z, int y) {
        for (int h = 0; h < 12; h++) { p.set(x, y + h, z, DARK); p.set(x + 3, y + h, z, DARK); p.set(x, y + h, z + 3, DARK); p.set(x + 3, y + h, z + 3, DARK); }
        for (int h = 0; h < 12; h++) p.set(x + 1, y + h, z + 1, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
        p.box(x - 1, y + 12, z - 1, x + 4, y + 12, z + 4, Blocks.SPRUCE_PLANKS.getDefaultState());
        p.set(x + 1, y + 12, z + 1, Blocks.SPRUCE_TRAPDOOR.getDefaultState());
        p.walls(x - 1, y + 13, z - 1, x + 4, y + 13, z + 4, Blocks.SAND.getDefaultState().getBlock() == Blocks.SAND ? SAND : SAND);
        p.box(x - 1, y + 16, z - 1, x + 4, y + 16, z + 4, Blocks.DARK_OAK_SLAB.getDefaultState());
        for (int[] c : new int[][]{{-1, -1}, {4, -1}, {-1, 4}, {4, 4}}) for (int h = 14; h < 16; h++) p.set(x + c[0], y + h, z + c[1], Blocks.DARK_OAK_FENCE.getDefaultState());
        p.set(x + 2, y + 15, z + 2, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    static void building(Placer p, int x0, int z0, int w, int d, int h, int y, BlockState wall) {
        int x1 = x0 + w - 1, z1 = z0 + d - 1;
        p.box(x0, y - 1, z0, x1, y - 1, z1, CONCRETE);
        for (int yy = 0; yy < h; yy++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) continue;
            boolean win = (yy % 5 == 2) && (x - x0) % 3 == 1 && !(x == x0 || x == x1);
            p.set(x, y + yy, z, win ? Blocks.GLASS_PANE.getDefaultState() : wall);
        }
        if (h > 6) p.box(x0 + 1, y + 5, z0 + 1, x1 - 1, y + 5, z1 - 1, CONCRETE);
        p.box(x0, y + h, z0, x1, y + h, z1, DARK);
        int mx = (x0 + x1) / 2;
        p.air(mx, y, z1, mx + 1, y + 2, z1);
        p.set(mx, y + 3, z1 + 1, Blocks.SEA_LANTERN.getDefaultState());
        for (int k = 0; k < w; k += 4) p.set(x0 + 2 + k, y + h - 1, z0 + 2, Blocks.SEA_LANTERN.getDefaultState());
        // Betten in der Kaserne / Tische im HQ
        for (int k = 1; k < w - 2; k += 3) p.set(x0 + k, y, z0 + 1, Blocks.GREEN_CARPET.getDefaultState());
    }

    static void helipad(Placer p, int x0, int z0, int y) {
        for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) {
            double dx = x - 7, dz = z - 7, r = Math.sqrt(dx * dx + dz * dz);
            if (r > 7.5) continue;
            BlockState s = r > 6.4 ? Y : CONCRETE;
            p.set(x0 + x, y - 1, z0 + z, s);
        }
        for (int k = -3; k <= 3; k++) { p.set(x0 + 5, y - 1, z0 + 7 + k, WHITE); p.set(x0 + 9, y - 1, z0 + 7 + k, WHITE); }
        for (int k = 6; k <= 8; k++) p.set(x0 + k, y - 1, z0 + 7, WHITE);
        for (int[] c : new int[][]{{0, 7}, {14, 7}, {7, 0}, {7, 14}}) p.set(x0 + c[0], y - 1, z0 + c[1], Blocks.SEA_LANTERN.getDefaultState());
    }

    static void details(Placer p, int x0, int z0, int y) {
        int gz = gateZ(z0);
        // Sandsäcke am Tor (innen)
        for (int k = -5; k <= 5; k++) if (Math.abs(k) > 2) {
            p.set(x0 + 4, y, gz + k, SAND);
            p.set(x0 + 4, y + 1, gz + k, Blocks.MUD_BRICK_SLAB.getDefaultState());
        }
        // Fahnenmast
        for (int h = 0; h < 10; h++) p.set(x0 + 28, y + h, gz + 6, Blocks.IRON_BARS.getDefaultState());
        p.box(x0 + 29, y + 7, gz + 6, x0 + 31, y + 9, gz + 6, Blocks.WHITE_WOOL.getDefaultState());
        p.box(x0 + 29, y + 8, gz + 6, x0 + 31, y + 8, gz + 6, Blocks.RED_WOOL.getDefaultState());
        // Kisten und Fässer
        for (int k = 0; k < 6; k++) {
            p.set(x0 + 30 + k, y, z0 + 3, k % 2 == 0 ? Blocks.BARREL.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState());
            if (k < 3) p.set(x0 + 30 + k, y + 1, z0 + 3, Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        // Flutlichter an der Mauer
        for (int x = x0 + 6; x < x0 + W - 4; x += 10) {
            p.set(x, y + 5, z0 + 1, Blocks.SEA_LANTERN.getDefaultState());
            p.set(x, y + 5, z0 + D - 2, Blocks.SEA_LANTERN.getDefaultState());
        }
    }
}
