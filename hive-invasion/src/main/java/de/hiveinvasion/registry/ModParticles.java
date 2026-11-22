package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
    public static final SimpleParticleType BLOOD = register("blood");
    public static final SimpleParticleType ACID_BLOOD = register("acid_blood");
    public static final SimpleParticleType PLASMA = register("plasma");

    private ModParticles() {}

    private static SimpleParticleType register(String name) {
        return Registry.register(Registries.PARTICLE_TYPE, HiveInvasion.id(name), FabricParticleTypes.simple());
    }

    public static void register() {}
}
