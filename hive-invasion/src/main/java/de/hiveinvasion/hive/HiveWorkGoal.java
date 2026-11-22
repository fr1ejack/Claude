package de.hiveinvasion.hive;

import de.hiveinvasion.entity.StalkerEntity;
import de.hiveinvasion.registry.ModBlocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

/**
 * Arbeit im Nest: Eier von der Königin in die Eierkammer tragen, oder aus der Eierkammer
 * zu den Gefangenen in der Kokonkammer. Sonst in der Ruhekammer herumlaufen.
 */
public class HiveWorkGoal extends Goal {
    private enum Phase { TO_SOURCE, TO_DEST, HOME }
    private final StalkerEntity s;
    private Phase phase;
    private BlockPos source, dest;
    private boolean toCocoons;
    private int timeout, repath;

    public HiveWorkGoal(StalkerEntity s) {
        this.s = s;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (s.getQueenPos() == null || s.getTarget() != null) return false;
        if (!s.getLoungePos().isWithinDistance(s.getPos(), 40)) return false;
        if (s.getRandom().nextInt(120) != 0) {
            if (!s.getLoungePos().isWithinDistance(s.getPos(), 9) && s.getRandom().nextInt(40) == 0) { phase = Phase.HOME; return true; }
            return false;
        }
        toCocoons = s.getRandom().nextBoolean();
        source = toCocoons ? s.getEggsPos() : s.getQueenPos();
        dest = toCocoons ? s.getCocoonsPos() : s.getEggsPos();
        phase = Phase.TO_SOURCE;
        return true;
    }

    @Override public void start() { timeout = 1600; repath = 0; }
    @Override public boolean shouldContinue() { return s.getTarget() == null && --timeout > 0 && phase != null; }
    @Override public void stop() { s.getNavigation().stop(); if (phase != Phase.TO_DEST) setCarrying(false); phase = null; }

    private void setCarrying(boolean c) { s.setCarrying(c); }

    @Override
    public void tick() {
        BlockPos goal = phase == Phase.TO_SOURCE ? source : phase == Phase.TO_DEST ? dest : s.getLoungePos();
        if (--repath <= 0) {
            repath = 30;
            s.getNavigation().startMovingTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, phase == Phase.TO_DEST ? 0.8 : 1.0);
        }
        if (!goal.isWithinDistance(s.getPos(), phase == Phase.HOME ? 4 : 5)) return;
        ServerWorld world = (ServerWorld) s.getWorld();
        switch (phase) {
            case TO_SOURCE -> {
                if (toCocoons) {   // Ei aus der Eierkammer nehmen
                    BlockPos pod = findPod(world, source, 7);
                    if (pod == null) { phase = null; return; }
                    world.removeBlock(pod, false);
                } else {
                    BlockPos pod = findPod(world, source, 8);
                    if (pod != null) world.removeBlock(pod, false);
                }
                world.playSound(null, s.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.HOSTILE, 0.8f, 0.5f);
                setCarrying(true);
                phase = Phase.TO_DEST;
                repath = 0;
            }
            case TO_DEST -> {
                BlockPos at = dest;
                if (toCocoons) {   // vor einen Gefangenen legen
                    List<CocoonEntity> cocoons = world.getEntitiesByClass(CocoonEntity.class, new net.minecraft.util.math.Box(dest).expand(10), c -> true);
                    if (!cocoons.isEmpty()) {
                        CocoonEntity c = cocoons.get(s.getRandom().nextInt(cocoons.size()));
                        Vec3d inFront = c.getPos().add(Vec3d.fromPolar(0, c.getYaw()).multiply(1.6));
                        at = BlockPos.ofFloored(inFront);
                    }
                }
                place(world, at);
                setCarrying(false);
                phase = Phase.HOME;
                repath = 0;
            }
            case HOME -> phase = null;
        }
    }

    private static BlockPos findPod(ServerWorld world, BlockPos c, int r) {
        for (BlockPos p : BlockPos.iterateOutwards(c, r, 4, r)) if (world.getBlockState(p).isOf(ModBlocks.BROOD_POD)) return p.toImmutable();
        return null;
    }

    private void place(ServerWorld world, BlockPos near) {
        for (BlockPos p : BlockPos.iterateOutwards(near, 2, 3, 2)) {
            if (world.getBlockState(p).isAir() && world.getBlockState(p.down()).isSideSolidFullSquare(world, p.down(), Direction.UP)) {
                world.setBlockState(p, ModBlocks.BROOD_POD.getDefaultState());
                world.playSound(null, p, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.HOSTILE, 1f, 0.6f);
                return;
            }
        }
    }
}
