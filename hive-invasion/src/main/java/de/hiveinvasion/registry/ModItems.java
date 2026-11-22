package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItems {
    public static final Item CLINGER_SPAWN_EGG = register("clinger_spawn_egg",
            new SpawnEggItem(ModEntities.CLINGER, 0xC9B79C, 0x6E5B47, new Item.Settings()));
    public static final Item LARVA_SPAWN_EGG = register("larva_spawn_egg",
            new SpawnEggItem(ModEntities.LARVA, 0xD8C8B0, 0x8A1C1C, new Item.Settings()));
    public static final Item STALKER_SPAWN_EGG = register("stalker_spawn_egg",
            new SpawnEggItem(ModEntities.STALKER, 0x14161C, 0x3D4C5C, new Item.Settings()));

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, HiveInvasion.id("main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.hiveinvasion.main"))
                    .icon(() -> new ItemStack(ModBlocks.BROOD_POD))
                    .entries((ctx, entries) -> {
                        entries.add(ModBlocks.BROOD_POD);
                        entries.add(ModBlocks.HIVE_RESIN);
                        entries.add(CLINGER_SPAWN_EGG);
                        entries.add(LARVA_SPAWN_EGG);
                        entries.add(STALKER_SPAWN_EGG);
                    })
                    .build());

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, HiveInvasion.id(name), item);
    }

    public static void register() {}
}
