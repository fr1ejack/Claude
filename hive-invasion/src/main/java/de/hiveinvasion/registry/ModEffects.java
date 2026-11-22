package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.effect.ParasiteEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModEffects {
    public static final RegistryEntry<StatusEffect> PARASITE =
            Registry.registerReference(Registries.STATUS_EFFECT, HiveInvasion.id("parasite"), new ParasiteEffect());

    private ModEffects() {}

    public static void register() {}
}
