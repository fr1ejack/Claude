package de.hiveinvasion.human;

import de.hiveinvasion.registry.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Schuss mit Sofort-Treffer: Leuchtspur, Mündungsfeuer, Einschlag, Schaden. */
public final class Firearms {
    private Firearms() {}

    public static void shoot(ServerWorld world, LivingEntity shooter, Vec3d dir, float spread, float damage, double range) {
        Vec3d start = shooter.getEyePos().add(dir.multiply(0.6)).add(0, -0.15, 0);
        var r = shooter.getRandom();
        Vec3d d = dir.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
        Vec3d end = start.add(d.multiply(range));
        BlockHitResult block = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, shooter));
        if (block.getType() != HitResult.Type.MISS) end = block.getPos();
        EntityHitResult hit = ProjectileUtil.raycast(shooter, start, end, shooter.getBoundingBox().stretch(d.multiply(range)).expand(1),
                e -> e instanceof LivingEntity && e.isAlive() && e != shooter && !e.isConnectedThroughVehicle(shooter)
                        && !(shooter instanceof SoldierEntity && e instanceof SoldierEntity), range * range);
        if (hit != null) {
            end = hit.getPos();
            Entity target = hit.getEntity();
            DamageSource src = shooter instanceof PlayerEntity p ? world.getDamageSources().playerAttack(p)
                    : world.getDamageSources().mobAttack(shooter);
            target.timeUntilRegen = 0;
            target.damage(src, damage);
        } else if (block.getType() == HitResult.Type.BLOCK) {
            var state = world.getBlockState(block.getBlockPos());
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), end.x, end.y, end.z, 6, 0.05, 0.05, 0.05, 0.1);
            world.spawnParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.02, 0.02, 0.02, 0.01);
        }
        // Leuchtspur
        Vec3d step = end.subtract(start);
        double len = step.length();
        step = step.normalize();
        for (double t = 0.8; t < len; t += 0.9) {
            Vec3d p = start.add(step.multiply(t));
            world.spawnParticles(ModParticles.TRACER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.spawnParticles(ParticleTypes.SMALL_FLAME, start.x, start.y, start.z, 2, 0.03, 0.03, 0.03, 0.01);
        world.spawnParticles(ParticleTypes.SMOKE, start.x, start.y, start.z, 1, 0.02, 0.02, 0.02, 0.01);
        world.playSound(null, shooter.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.HOSTILE, 1.6f, 0.55f + r.nextFloat() * 0.1f);
        world.playSound(null, shooter.getBlockPos(), SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.HOSTILE, 0.5f, 1.9f);
    }
}
