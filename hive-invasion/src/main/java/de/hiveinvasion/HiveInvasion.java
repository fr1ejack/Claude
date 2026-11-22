package de.hiveinvasion;

import de.hiveinvasion.entity.ClingerEntity;
import de.hiveinvasion.entity.CorpseEntity;
import de.hiveinvasion.exo.ExoSuitEntity;
import de.hiveinvasion.fx.BloodFx;
import de.hiveinvasion.net.ExoInputPayload;
import de.hiveinvasion.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.damage.DamageTypes;
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
        ModParticles.register();
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();

        de.hiveinvasion.gore.Gore.register();
        de.hiveinvasion.world.WorldDirector.register();
        de.hiveinvasion.military.MilitaryNet.register();
        de.hiveinvasion.world.HiveCommands.register();
        PayloadTypeRegistry.playS2C().register(de.hiveinvasion.net.ScanPayload.ID, de.hiveinvasion.net.ScanPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(de.hiveinvasion.gore.SeveredPayload.ID, de.hiveinvasion.gore.SeveredPayload.CODEC);

        // Steuerung des Exo-Anzugs vom Client.
        PayloadTypeRegistry.playC2S().register(ExoInputPayload.ID, ExoInputPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ExoInputPayload.ID, (payload, ctx) -> {
            if (ctx.player().getVehicle() instanceof ExoSuitEntity suit) {
                suit.setInput(payload.fire(), payload.flame(), payload.shield(), payload.boost());
            }
        });

        PayloadTypeRegistry.playC2S().register(de.hiveinvasion.exo.ExoEnterPayload.ID, de.hiveinvasion.exo.ExoEnterPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(de.hiveinvasion.exo.ExoEnterPayload.ID, (payload, ctx) -> {
            var player = ctx.player();
            if (player.getWorld().getEntityById(payload.suitId()) instanceof ExoSuitEntity suit
                    && suit.canBeEntered() && player.squaredDistanceTo(suit) < 9 && !player.hasVehicle()) {
                suit.beginAssembly(player);
            }
        });

        // Wer im Exo-Anzug sitzt, wird vom Anzug geschützt: der Schaden geht auf den Anzug.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity && entity.getVehicle() instanceof ExoSuitEntity suit
                    && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && !source.isOf(DamageTypes.STARVE) && !source.isOf(DamageTypes.OUT_OF_WORLD)
                    && !source.getTypeRegistryEntry().matchesKey(de.hiveinvasion.effect.ParasiteEffect.BURST)) {
                if (!source.isOf(DamageTypes.DROWN) && !source.isOf(DamageTypes.FALL)) suit.damage(source, amount);
                return false;
            }
            return true;
        });

        // Blut
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked) {
                BloodFx.onHurt(entity, damageTaken);
                de.hiveinvasion.gore.Gore.afterHit(entity, source, damageTaken);
            }
        });

        // Leichen bleiben liegen
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            BloodFx.onDeath(entity);
            if (leavesCorpse(entity)) {
                entity.getWorld().spawnEntity(CorpseEntity.of(ModEntities.CORPSE, entity));
            }
        });

        LOGGER.info("Hive Invasion geladen – die Brut erwacht.");
    }

    private static boolean leavesCorpse(LivingEntity e) {
        if (e instanceof PlayerEntity) return !e.isSpectator();
        if (!(e instanceof MobEntity)) return false;
        if (e instanceof EnderDragonEntity || e instanceof WitherEntity || e instanceof SlimeEntity
                || e instanceof VexEntity || e instanceof AllayEntity || e instanceof BatEntity
                || e instanceof GhastEntity || e instanceof PhantomEntity || e instanceof ClingerEntity
                || e instanceof ExoSuitEntity || e instanceof de.hiveinvasion.hive.CocoonEntity
                || e instanceof de.hiveinvasion.hive.QueenEntity) return false;
        float w = e.getWidth();
        return w >= 0.35f && w <= 2.0f;
    }
}
