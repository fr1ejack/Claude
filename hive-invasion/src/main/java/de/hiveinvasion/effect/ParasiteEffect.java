package de.hiveinvasion.effect;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.entity.LarvaEntity;
import de.hiveinvasion.registry.ModEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Parasit: eine Larve wächst im Wirt. Gegen Ende wird dem Wirt übel,
 * wenn der Effekt abläuft, bricht die Larve heraus.
 */
public class ParasiteEffect extends StatusEffect {
    public static final RegistryKey<DamageType> BURST =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, HiveInvasion.id("burst"));

    public ParasiteEffect() {
        super(StatusEffectCategory.HARMFUL, 0x5A1A1A);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration == 1 || (duration <= 600 && duration % 100 == 0);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return true;
        StatusEffectInstance self = entity.getStatusEffect(de.hiveinvasion.registry.ModEffects.PARASITE);
        int remaining = self == null ? 1 : self.getDuration();

        if (remaining > 1) {
            // Letzte 30 Sekunden: Übelkeit, Krämpfe.
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 120, 1));
            if (entity instanceof PlayerEntity player) {
                player.sendMessage(Text.translatable("message.hiveinvasion.parasite_moving")
                        .formatted(Formatting.RED, Formatting.ITALIC), true);
            }
            world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH,
                    SoundCategory.HOSTILE, 0.6f, 0.5f);
            return true;
        }

        // Die Larve bricht heraus.
        LarvaEntity larva = ModEntities.LARVA.create(world);
        if (larva != null) {
            larva.refreshPositionAndAngles(entity.getX(), entity.getBodyY(0.5), entity.getZ(),
                    entity.getYaw(), 0.0f);
            larva.initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.CONVERSION, null);
            world.spawnEntity(larva);
        }
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getBodyY(0.6), entity.getZ(),
                25, 0.3, 0.3, 0.3, 0.2);
        world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.5f, 0.5f);
        DamageSource burst = new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(BURST));
        entity.damage(burst, 18.0f);
        return true;
    }
}
