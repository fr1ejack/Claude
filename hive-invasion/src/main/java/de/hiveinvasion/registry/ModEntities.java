package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.entity.ClingerEntity;
import de.hiveinvasion.entity.CorpseEntity;
import de.hiveinvasion.exo.ExoSuitEntity;
import de.hiveinvasion.exo.PlasmaBoltEntity;
import de.hiveinvasion.entity.LarvaEntity;
import de.hiveinvasion.entity.StalkerEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;

public final class ModEntities {
    public static final EntityType<ClingerEntity> CLINGER = register("clinger",
            EntityType.Builder.create(ClingerEntity::new, SpawnGroup.MONSTER).dimensions(0.7f, 0.35f).maxTrackingRange(8));

    public static final EntityType<LarvaEntity> LARVA = register("larva",
            EntityType.Builder.create(LarvaEntity::new, SpawnGroup.MONSTER).dimensions(0.4f, 0.3f).maxTrackingRange(8));

    public static final EntityType<StalkerEntity> STALKER = register("stalker",
            EntityType.Builder.create(StalkerEntity::new, SpawnGroup.MONSTER).dimensions(0.9f, 2.2f).maxTrackingRange(10));

    public static final EntityType<CorpseEntity> CORPSE = register("corpse",
            EntityType.Builder.<CorpseEntity>create(CorpseEntity::new, SpawnGroup.MISC).dimensions(1.4f, 0.45f).maxTrackingRange(8));

    public static final EntityType<ExoSuitEntity> EXO_SUIT = register("exo_suit",
            EntityType.Builder.create(ExoSuitEntity::new, SpawnGroup.MISC).dimensions(1.0f, 2.7f).maxTrackingRange(10));

    public static final EntityType<PlasmaBoltEntity> PLASMA_BOLT = register("plasma_bolt",
            EntityType.Builder.<PlasmaBoltEntity>create(PlasmaBoltEntity::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f)
                    .maxTrackingRange(6).trackingTickInterval(1));

    private ModEntities() {}

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, HiveInvasion.id(name), builder.build(name));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(CLINGER, ClingerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LARVA, LarvaEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(EXO_SUIT, ExoSuitEntity.createAttributes());

        SpawnRestriction.register(STALKER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                HostileEntity::canSpawnInDark);
        // Selten nachts/in Höhlen in der Oberwelt – die richtigen Horden kommen später aus den Nestern.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, STALKER, 8, 1, 2);
    }
}
