package de.hiveinvasion.entity;

import de.hiveinvasion.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Larve: schlüpft aus dem Wirt, flieht und wächst zu einem Stalker heran. */
public class LarvaEntity extends HostileEntity implements Alien {
    /** Zeit bis zum ausgewachsenen Stalker (Ticks). 3600 = 3 Minuten. */
    public static final int GROW_TICKS = 3600;
    private int growth;

    public LarvaEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 10.0f, 1.0, 1.4, Alien::isPrey));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(5, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld world && this.isAlive()) {
            if (++this.growth >= GROW_TICKS) {
                StalkerEntity adult = this.convertTo(ModEntities.STALKER, false);
                if (adult != null) {
                    world.spawnParticles(ParticleTypes.SQUID_INK, adult.getX(), adult.getBodyY(0.5), adult.getZ(),
                            20, 0.4, 0.8, 0.4, 0.02);
                    world.playSound(null, adult.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR,
                            SoundCategory.HOSTILE, 0.8f, 1.6f);
                }
            }
        }
    }

    public void setGrowth(int growth) { this.growth = growth; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Growth", this.growth);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.growth = nbt.getInt("Growth");
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt) Alien.splashAcid(this, source);
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_SILVERFISH_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_SILVERFISH_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SILVERFISH_DEATH; }
}
