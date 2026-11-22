package de.hiveinvasion.fx;

import de.hiveinvasion.entity.Alien;
import de.hiveinvasion.registry.ModBlocks;
import de.hiveinvasion.registry.ModParticles;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Blutspritzer und Blutlachen. */
public final class BloodFx {
    private BloodFx() {}

    public enum Kind { NONE, RED, ACID }

    public static Kind kindOf(LivingEntity e) {
        if (e instanceof Alien) return Kind.ACID;
        if (e instanceof AbstractSkeletonEntity || e instanceof IronGolemEntity || e instanceof SnowGolemEntity
                || e instanceof BlazeEntity || e instanceof SlimeEntity || e instanceof VexEntity
                || e instanceof ArmorStandEntity || e instanceof ShulkerEntity || e instanceof GhastEntity
                || e.getType().getSpawnGroup().name().contains("WATER")) return Kind.NONE;
        return Kind.RED;
    }

    /** Spritzer beim Treffer, Menge abhängig vom Schaden. */
    public static void onHurt(LivingEntity e, float damage) {
        if (!(e.getWorld() instanceof ServerWorld world) || damage <= 0) return;
        Kind kind = kindOf(e);
        if (kind == Kind.NONE) return;
        SimpleParticleType type = kind == Kind.ACID ? ModParticles.ACID_BLOOD : ModParticles.BLOOD;
        Random r = e.getRandom();
        int n = Math.min(40, 4 + (int) (damage * 3));
        for (int i = 0; i < n; i++) {
            double vx = (r.nextDouble() - 0.5) * 0.5, vy = r.nextDouble() * 0.35 + 0.05, vz = (r.nextDouble() - 0.5) * 0.5;
            // count = 0 → genau ein Partikel mit dieser Geschwindigkeit
            world.spawnParticles(type, e.getX(), e.getBodyY(0.55 + r.nextDouble() * 0.3), e.getZ(), 0, vx, vy, vz, 1.0);
        }
        if (damage >= 4 && r.nextFloat() < 0.35f) puddles(world, e.getBlockPos(), kind, 1, 1);
    }

    public static void onDeath(LivingEntity e) {
        if (!(e.getWorld() instanceof ServerWorld world)) return;
        Kind kind = kindOf(e);
        if (kind == Kind.NONE) return;
        puddles(world, e.getBlockPos(), kind, 1, 3 + e.getRandom().nextInt(3));
    }

    public static void puddles(ServerWorld world, BlockPos center, Kind kind, int radius, int count) {
        BlockState puddle = (kind == Kind.ACID ? ModBlocks.ACID_PUDDLE : ModBlocks.BLOOD_PUDDLE).getDefaultState();
        Random r = world.random;
        for (int i = 0; i < count * 3 && count > 0; i++) {
            BlockPos p = center.add(r.nextInt(radius * 2 + 1) - radius, 0, r.nextInt(radius * 2 + 1) - radius);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos q = p.up(dy);
                if (world.getBlockState(q).isAir() && puddle.canPlaceAt(world, q)) {
                    world.setBlockState(q, puddle);
                    count--;
                    break;
                }
            }
        }
    }
}
