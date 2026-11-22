package de.hiveinvasion.hive;

import de.hiveinvasion.human.CivilianEntity;
import de.hiveinvasion.registry.ModEffects;
import de.hiveinvasion.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Ein lebender Mensch, in Harz an die Nestwand geklebt. Wird von den Klammerern als Wirt benutzt.
 * Schlägt ein Spieler den Kokon auf, bevor der Gefangene infiziert ist, wird er befreit.
 */
public class CocoonEntity extends MobEntity {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(CocoonEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private int struggle;

    public CocoonEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0);
    }

    public int getVariant() { return dataTracker.get(VARIANT); }
    public void setVariant(int v) { dataTracker.set(VARIANT, Math.floorMod(v, 10)); }
    public int getStruggle() { return struggle; }

    @Override
    public void tick() {
        super.tick();
        setVelocity(0, 0, 0);
        if (getWorld().isClient) {
            if (struggle > 0) struggle--;
            else if (random.nextInt(160) == 0) struggle = 30;   // zappelt ab und zu
        } else if (random.nextInt(400) == 0) {
            getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.NEUTRAL, 0.4f, 0.7f);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity player && !getWorld().isClient
                && !hasStatusEffect(ModEffects.PARASITE)) {
            free((ServerWorld) getWorld(), player);
            return true;
        }
        return super.damage(source, amount);
    }

    private void free(ServerWorld world, PlayerEntity rescuer) {
        CivilianEntity civ = ModEntities.CIVILIAN.create(world);
        if (civ != null) {
            civ.refreshPositionAndAngles(getX(), getY() - 0.5, getZ(), getYaw(), 0);
            civ.initialize(world, world.getLocalDifficulty(getBlockPos()), SpawnReason.EVENT, null);
            civ.setVariant(getVariant());
            civ.setHome(rescuer.getBlockPos());
            world.spawnEntity(civ);
        }
        world.playSound(null, getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.NEUTRAL, 1.5f, 0.6f);
        rescuer.sendMessage(Text.translatable("message.hiveinvasion.rescued").formatted(Formatting.GREEN), true);
        discard();
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void pushAway(net.minecraft.entity.Entity entity) {}
    @Override public boolean cannotDespawn() { return true; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_PLAYER_DEATH; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) { super.writeCustomDataToNbt(nbt); nbt.putInt("Skin", getVariant()); }
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) { super.readCustomDataFromNbt(nbt); setVariant(nbt.getInt("Skin")); }
}
