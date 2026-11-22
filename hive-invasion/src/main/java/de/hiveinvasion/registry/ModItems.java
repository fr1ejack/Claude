package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.exo.ExoSuitItem;
import de.hiveinvasion.exo.ExoTriggerItem;
import net.minecraft.util.Rarity;
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

    public static final Item EXO_SUIT = register("exo_suit",
            new ExoSuitItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item EXO_TRIGGER = register("exo_trigger",
            new ExoTriggerItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item EXO_RACK = register("exo_rack",
            new de.hiveinvasion.exo.ExoRackItem(new Item.Settings().maxCount(16)));
    public static final Item PLASMA_BOLT = register("plasma_bolt", new Item(new Item.Settings()));

    public static final Item COMBAT_KNIFE = register("combat_knife", new net.minecraft.item.SwordItem(
            net.minecraft.item.ToolMaterials.IRON, new Item.Settings().attributeModifiers(
                    net.minecraft.item.SwordItem.createAttributeModifiers(net.minecraft.item.ToolMaterials.IRON, 1, -1.6f))));

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, HiveInvasion.id("main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.hiveinvasion.main"))
                    .icon(() -> new ItemStack(ModBlocks.BROOD_POD))
                    .entries((ctx, entries) -> {
                        entries.add(ModBlocks.BROOD_POD);
                        entries.add(ModBlocks.HIVE_RESIN);
                        entries.add(COMBAT_KNIFE);
                        entries.add(EXO_SUIT);
                        entries.add(EXO_RACK);
                        entries.add(EXO_TRIGGER);
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
