package de.hiveinvasion.entity;

import de.hiveinvasion.registry.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Klammerer: springt aus der Brutkapsel, klammert sich an das Gesicht des Opfers
 * und pflanzt eine Larve ein. Danach stirbt er.
 */
public class ClingerEntity extends HostileEntity implements Alien {
    /** Zeit bis die Larve schlüpft (Ticks). 2400 = 2 Minuten. */
    public static final int INCUBATION_TICKS = 2400;

    public ClingerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 3;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new PounceAtTargetGoal(this, 0.45f));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true, Alien::canHost));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, true,
                e -> Alien.canHost(e) && !(e instanceof HostileEntity)));
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof LivingEntity victim && Alien.canHost(victim) && !this.getWorld().isClient) {
            victim.addStatusEffect(new StatusEffectInstance(ModEffects.PARASITE, INCUBATION_TICKS, 0));
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 160, 0));
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
            if (victim instanceof PlayerEntity player) {
                player.sendMessage(Text.translatable("message.hiveinvasion.clinger_attached")
                        .formatted(Formatting.DARK_RED), true);
            }
            this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SPIDER_HURT,
                    SoundCategory.HOSTILE, 1.0f, 0.6f);
            this.discard();
            return true;
        }
        return super.tryAttack(target);
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
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_SPIDER_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SPIDER_DEATH; }
}
