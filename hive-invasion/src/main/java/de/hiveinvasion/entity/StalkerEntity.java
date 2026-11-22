package de.hiveinvasion.entity;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Stalker: das ausgewachsene Alien. Schnell, klettert Wände hoch,
 * jagt im Dunkeln und hat Säureblut.
 */
public class StalkerEntity extends HostileEntity implements Alien {
    // Vergleichs-Basis "Soldat": 32 Leben, 6 Schaden – Aliens sind ALIEN_STRENGTH-mal so stark.
    private static final double BASE_HEALTH = 32.0;
    private static final double BASE_DAMAGE = 6.0;

    public StalkerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, BASE_HEALTH * HiveInvasion.ALIEN_STRENGTH)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, BASE_DAMAGE * HiveInvasion.ALIEN_STRENGTH)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(3, new PounceAtTargetGoal(this, 0.5f));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true, Alien::isPrey));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new SpiderNavigation(this, world);
    }

    /** Klettert an jeder Wand hoch, gegen die er läuft. */
    @Override
    public boolean isClimbing() {
        return this.horizontalCollision || super.isClimbing();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt) Alien.splashAcid(this, source);
        return hurt;
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !(target instanceof Alien) && super.canTarget(target);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_SPIDER_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_RAVAGER_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_RAVAGER_DEATH; }
    @Override
    public float getSoundPitch() { return 1.5f; }
}
