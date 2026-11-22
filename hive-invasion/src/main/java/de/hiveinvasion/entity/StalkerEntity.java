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
    private static final net.minecraft.entity.data.TrackedData<Boolean> CARRYING =
            net.minecraft.entity.data.DataTracker.registerData(StalkerEntity.class, net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @org.jetbrains.annotations.Nullable private net.minecraft.util.math.BlockPos queenPos, eggsPos, cocoonsPos, loungePos, raidTarget;
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
    protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CARRYING, false);
    }

    public boolean isCarrying() { return dataTracker.get(CARRYING); }
    public void setCarrying(boolean c) { dataTracker.set(CARRYING, c); }

    public void setHive(de.hiveinvasion.hive.HiveBuilder.Layout l) {
        queenPos = l.queen; eggsPos = l.eggs; cocoonsPos = l.cocoons; loungePos = l.lounge1;
    }

    public void setRaidTarget(net.minecraft.util.math.BlockPos target) { this.raidTarget = target; }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(5, new de.hiveinvasion.hive.HiveWorkGoal(this));
        this.goalSelector.add(5, new RaidGoal(this));
        this.goalSelector.add(3, new PounceAtTargetGoal(this, 0.5f));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true, Alien::isPrey));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, de.hiveinvasion.human.HumanEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    public net.minecraft.util.math.BlockPos getQueenPos() { return queenPos; }
    public net.minecraft.util.math.BlockPos getEggsPos() { return eggsPos; }
    public net.minecraft.util.math.BlockPos getCocoonsPos() { return cocoonsPos; }
    public net.minecraft.util.math.BlockPos getLoungePos() { return loungePos; }

    /** Angriffswelle: zur Stadt marschieren, dort jagen. */
    static class RaidGoal extends Goal {
        private final StalkerEntity s;
        RaidGoal(StalkerEntity s) { this.s = s; setControls(java.util.EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return s.raidTarget != null && s.getTarget() == null; }
        @Override public void tick() {
            if (s.raidTarget.isWithinDistance(s.getPos(), 24)) { s.raidTarget = null; return; }
            if (s.getNavigation().isIdle() || s.age % 40 == 0)
                de.hiveinvasion.human.Navigation.moveToward(s, net.minecraft.util.math.Vec3d.ofBottomCenter(s.raidTarget), 1.15);
        }
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (queenPos != null) { nbt.putLong("Queen", queenPos.asLong()); nbt.putLong("Eggs", eggsPos.asLong());
            nbt.putLong("Cocoons", cocoonsPos.asLong()); nbt.putLong("Lounge", loungePos.asLong()); }
        if (raidTarget != null) nbt.putLong("Raid", raidTarget.asLong());
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Queen")) { queenPos = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("Queen"));
            eggsPos = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("Eggs"));
            cocoonsPos = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("Cocoons"));
            loungePos = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("Lounge")); }
        if (nbt.contains("Raid")) raidTarget = net.minecraft.util.math.BlockPos.fromLong(nbt.getLong("Raid"));
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
