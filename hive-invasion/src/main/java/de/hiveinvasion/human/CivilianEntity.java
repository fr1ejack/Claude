package de.hiveinvasion.human;

import de.hiveinvasion.entity.StalkerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Zivilist: läuft in der Stadt herum, flieht vor Aliens, kann infiziert werden. */
public class CivilianEntity extends HumanEntity {
    public CivilianEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override public int variantCount() { return 10; }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new FleeEntityGoal<>(this, StalkerEntity.class, 20f, 1.0, 1.45));
        goalSelector.add(1, new FleeEntityGoal<>(this, LivingEntity.class, 10f, 1.0, 1.4, e -> e instanceof de.hiveinvasion.entity.Alien));
        goalSelector.add(2, new EscapeDangerGoal(this, 1.45));
        goalSelector.add(3, new StayNearHomeGoal(this, 60, 0.9));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.75));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        goalSelector.add(7, new LookAtEntityGoal(this, HumanEntity.class, 6f));
        goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_PLAYER_DEATH; }
}
