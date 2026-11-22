package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.block.BroodPodBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    /** Brutkapsel: öffnet sich, wenn jemand zu nah kommt, und entlässt einen Klammerer. */
    public static final Block BROOD_POD = register("brood_pod", new BroodPodBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.TERRACOTTA_GREEN)
            .strength(1.5f)
            .sounds(BlockSoundGroup.SLIME)
            .nonOpaque()
            .ticksRandomly()));

    /** Nest-Harz: die Wände der Nester. */
    public static final Block HIVE_RESIN = register("hive_resin", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK)
            .strength(2.5f, 4.0f)
            .sounds(BlockSoundGroup.HONEY)));

    private ModBlocks() {}

    private static Block register(String name, Block block) {
        Registry.register(Registries.BLOCK, HiveInvasion.id(name), block);
        Registry.register(Registries.ITEM, HiveInvasion.id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void register() {}
}
