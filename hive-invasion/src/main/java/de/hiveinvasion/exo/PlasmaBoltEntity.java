package de.hiveinvasion.exo;

import de.hiveinvasion.registry.ModItems;
import de.hiveinvasion.registry.ModParticles;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Plasmageschoss der Exo-Kanone. Fliegt gerade, ohne Schwerkraft. */
public class PlasmaBoltEntity extends ThrownItemEntity {
    public static final float DAMAGE = 8.0f;

    public PlasmaBoltEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PLASMA_BOLT;
    }

    @Override
    protected double getGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            getWorld().addParticle(ModParticles.PLASMA, getX(), getY(), getZ(), 0, 0, 0);
        } else if (age > 60) {
            discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        hit.getEntity().damage(getDamageSources().thrown(this, getOwner()), DAMAGE);
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 10, 0.15, 0.15, 0.15, 0.2);
            world.spawnParticles(ModParticles.PLASMA, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.05);
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 0.4f, 1.8f);
            discard();
        }
    }
}
