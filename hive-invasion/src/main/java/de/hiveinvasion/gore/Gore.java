package de.hiveinvasion.gore;

import com.mojang.serialization.Codec;
import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.entity.CorpseEntity;
import de.hiveinvasion.entity.StalkerEntity;
import de.hiveinvasion.fx.BloodFx;
import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.registry.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Abtrennen von Körperteilen mit dem Kampfmesser.
 * Fehlende Teile werden pro Lebewesen gespeichert, an die Clients geschickt
 * und wirken sich aus (Bein ab = langsamer, Arm ab = schwächer, Kopf ab = tot).
 */
public final class Gore {
    private Gore() {}

    public static final AttachmentType<Integer> SEVERED =
            AttachmentRegistry.createPersistent(HiveInvasion.id("severed"), Codec.INT);

    public static void register() {
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                int mask = getMask(living);
                if (mask != 0) ServerPlayNetworking.send(player, new SeveredPayload(entity.getId(), mask));
            }
        });
    }

    public static int getMask(LivingEntity e) {
        Integer m = e.getAttached(SEVERED);
        return m == null ? 0 : m;
    }

    /** Lässt sich dieses Lebewesen zerlegen? (Modelle mit einzelnen Gliedern) */
    public static boolean canSever(LivingEntity e) {
        return e instanceof ZombieEntity || e instanceof AbstractSkeletonEntity || e instanceof AbstractPiglinEntity
                || e instanceof StalkerEntity || e instanceof de.hiveinvasion.human.HumanEntity;
    }

    public static boolean isKnife(ItemStack stack) {
        return stack.isOf(ModItems.COMBAT_KNIFE);
    }

    /** Nach einem Treffer: mit dem Messer evtl. ein Glied abtrennen. */
    public static void afterHit(LivingEntity victim, DamageSource source, float damage) {
        if (!(victim.getWorld() instanceof ServerWorld world) || !canSever(victim)) return;
        if (!(source.getAttacker() instanceof LivingEntity attacker) || source.getSource() != attacker) return;
        if (!isKnife(attacker.getMainHandStack())) return;
        Random r = victim.getRandom();
        boolean lethal = victim.isDead() || victim.getHealth() <= 0;
        float chance = lethal ? 0.85f : 0.35f;
        if (r.nextFloat() > chance) return;

        List<Limb> free = new ArrayList<>();
        int mask = getMask(victim);
        for (Limb l : Limb.values()) {
            if (l.in(mask)) continue;
            if (l == Limb.HEAD && !lethal && victim.getHealth() > victim.getMaxHealth() * 0.3f) continue;
            free.add(l);
        }
        if (free.isEmpty()) return;
        Limb limb = free.get(r.nextInt(free.size()));
        Vec3d away = victim.getPos().subtract(attacker.getPos()).multiply(1, 0, 1).normalize();
        sever(world, victim, limb, away);
        if (limb == Limb.HEAD && victim.isAlive()) victim.kill();
    }

    public static void sever(ServerWorld world, LivingEntity victim, Limb limb, Vec3d away) {
        int mask = getMask(victim) | limb.bit();
        victim.setAttached(SEVERED, mask);
        for (ServerPlayerEntity p : PlayerLookup.tracking(victim)) ServerPlayNetworking.send(p, new SeveredPayload(victim.getId(), mask));
        if (victim instanceof ServerPlayerEntity self) ServerPlayNetworking.send(self, new SeveredPayload(victim.getId(), mask));

        // Ist das Opfer schon tot, gibt es bereits eine Leiche – der fehlt das Teil dann auch.
        if (!victim.isAlive()) {
            for (CorpseEntity c : world.getEntitiesByClass(CorpseEntity.class, victim.getBoundingBox().expand(1.0), c -> c.getCorpseAge() < 3)) {
                c.addSevered(limb.bit());
            }
        }

        // Auswirkungen
        if (limb == Limb.RIGHT_LEG || limb == Limb.LEFT_LEG) {
            modify(victim.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), "leg_" + limb.ordinal(), -0.45);
        } else if (limb == Limb.RIGHT_ARM || limb == Limb.LEFT_ARM) {
            modify(victim.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), "arm_" + limb.ordinal(), -0.4);
            EquipmentSlot slot = limb == Limb.RIGHT_ARM ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            ItemStack held = victim.getEquippedStack(slot);
            if (!held.isEmpty() && !(victim instanceof PlayerEntity)) {
                victim.dropStack(held.copy());
                victim.equipStack(slot, ItemStack.EMPTY);
            }
        }

        // Das abgetrennte Teil fliegt weg
        SeveredLimbEntity part = SeveredLimbEntity.of(ModEntities.SEVERED_LIMB, victim, limb);
        Random r = victim.getRandom();
        part.setVelocity(away.x * 0.25 + (r.nextDouble() - 0.5) * 0.15, 0.25 + r.nextDouble() * 0.15,
                away.z * 0.25 + (r.nextDouble() - 0.5) * 0.15);
        world.spawnEntity(part);

        BloodFx.onHurt(victim, 12f);
        BloodFx.puddles(world, victim.getBlockPos(), BloodFx.kindOf(victim), 1, 2);
        world.playSound(null, victim.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 0.6f);
        world.playSound(null, victim.getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.PLAYERS, 1.2f, 0.5f);
    }

    private static void modify(EntityAttributeInstance attr, String name, double amount) {
        if (attr == null) return;
        var id = HiveInvasion.id("severed_" + name);
        if (attr.getModifier(id) == null) {
            attr.addPersistentModifier(new EntityAttributeModifier(id, amount, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
