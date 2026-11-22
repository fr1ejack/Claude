package de.hiveinvasion.registry;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.block.BloodPuddleBlock;
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

    public static final Block HIVE_RIBS = register("hive_ribs", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK).strength(2.5f, 4.0f).sounds(BlockSoundGroup.HONEY)));
    public static final Block HIVE_GLOW = register("hive_glow", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.LIME).strength(1.0f).sounds(BlockSoundGroup.SLIME).luminance(s -> 10)));

    /** Blutlachen – kein Item, entstehen nur durch Verletzungen. */
    public static final Block BLOOD_PUDDLE = registerNoItem("blood_puddle", new BloodPuddleBlock(puddle(MapColor.DARK_RED), false));
    public static final Block ACID_PUDDLE = registerNoItem("acid_puddle", new BloodPuddleBlock(puddle(MapColor.LIME).luminance(s -> 4), true));

    private ModBlocks() {}

    private static AbstractBlock.Settings puddle(MapColor color) {
        return AbstractBlock.Settings.create().mapColor(color).noCollision().breakInstantly().nonOpaque()
                .replaceable().dropsNothing().ticksRandomly().sounds(BlockSoundGroup.HONEY)
                .pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY);
    }

    private static Block registerNoItem(String name, Block block) {
        return Registry.register(Registries.BLOCK, HiveInvasion.id(name), block);
    }

    private static Block register(String name, Block block) {
        Registry.register(Registries.BLOCK, HiveInvasion.id(name), block);
        Registry.register(Registries.ITEM, HiveInvasion.id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void register() {}
}
