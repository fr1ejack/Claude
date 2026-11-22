package de.hiveinvasion.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Schnelles Setzen von Blöcken ohne Nachbar-Updates, mit Hilfsfunktionen für Kästen. */
public final class Placer {
    private final ServerWorld world;
    private final BlockPos.Mutable m = new BlockPos.Mutable();
    public int placed;

    public Placer(ServerWorld world) { this.world = world; }

    public ServerWorld world() { return world; }

    public void set(int x, int y, int z, BlockState s) {
        m.set(x, y, z);
        world.setBlockState(m, s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
        placed++;
    }

    public BlockState get(int x, int y, int z) { return world.getBlockState(m.set(x, y, z)); }

    public void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    /** Nur die Außenwände eines Quaders. */
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) { set(x, y, z0, s); set(x, y, z1, s); }
            for (int z = z0; z <= z1; z++) { set(x0, y, z, s); set(x1, y, z, s); }
        }
    }

    public void air(int x0, int y0, int z0, int x1, int y1, int z1) { box(x0, y0, z0, x1, y1, z1, Blocks.AIR.getDefaultState()); }
}
