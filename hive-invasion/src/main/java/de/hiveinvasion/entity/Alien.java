package de.hiveinvasion.entity;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import de.hiveinvasion.registry.ModEffects;

/** Gemeinsames Verhalten aller Aliens. */
public interface Alien {

    /** Kann dieses Lebewesen als Wirt / Beute dienen? */
    static boolean isPrey(LivingEntity e) {
        if (e instanceof Alien) return false;
        if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) return false;
        return e.isAlive();
    }

    static boolean canHost(LivingEntity e) {
        // Im geschlossenen Exo-Anzug ist man vor Klammerern sicher.
        if (e.getVehicle() instanceof de.hiveinvasion.exo.ExoSuitEntity) return false;
        return isPrey(e) && !e.hasStatusEffect(ModEffects.PARASITE);
    }

    /**
     * Säureblut: wer ein Alien im Nahkampf trifft, dessen Waffe wird angefressen,
     * und er bekommt etwas Säure ab.
     */
    static void splashAcid(LivingEntity alien, DamageSource source) {
        if (!(alien.getWorld() instanceof ServerWorld world)) return;
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;
        if (source.getSource() != attacker || attacker.squaredDistanceTo(alien) > 9.0) return;

        ItemStack weapon = attacker.getMainHandStack();
        if (!weapon.isEmpty() && weapon.isDamageable()) {
            weapon.damage(3, attacker, EquipmentSlot.MAINHAND);
        }
        if (alien.getRandom().nextFloat() < 0.35f) {
            attacker.damage(world.getDamageSources().magic(), 1.5f);
            attacker.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.POISON, 40, 0));
        }
        world.playSound(null, alien.getBlockPos(), SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 0.5f, 1.6f);
    }
}
