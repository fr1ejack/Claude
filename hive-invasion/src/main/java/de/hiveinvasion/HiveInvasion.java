package de.hiveinvasion;

import de.hiveinvasion.registry.ModBlocks;
import de.hiveinvasion.registry.ModEffects;
import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveInvasion implements ModInitializer {
    public static final String MOD_ID = "hiveinvasion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Wie viel stärker die Aliens als ein vergleichbarer Mensch/Soldat sind.
     * 1.25 = 25 % stärker (Leben und Schaden).
     */
    public static final float ALIEN_STRENGTH = 1.25f;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEffects.register();
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();
        LOGGER.info("Hive Invasion geladen – die Brut erwacht.");
    }
}
