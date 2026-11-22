package de.hiveinvasion.human;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Gemeinsame Basis für Zivilisten und Soldaten (Spielermodell, verschiedene Skins, Heimatort). */
public abstract class HumanEntity extends PathAwareEntity {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Nullable protected BlockPos home;

    protected HumanEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setCanPickUpLoot(false);
    }

    public abstract int variantCount();

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0);
    }

    public int getVariant() { return dataTracker.get(VARIANT); }
    public void setVariant(int v) { dataTracker.set(VARIANT, Math.floorMod(v, variantCount())); }

    public void setHome(BlockPos home) { this.home = home; }
    @Nullable public BlockPos getHome() { return home; }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason, @Nullable EntityData data) {
        setVariant(random.nextInt(variantCount()));
        if (home == null) home = getBlockPos();
        return super.initialize(world, difficulty, reason, data);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Skin", getVariant());
        if (home != null) nbt.putLong("Home", home.asLong());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setVariant(nbt.getInt("Skin"));
        if (nbt.contains("Home")) home = BlockPos.fromLong(nbt.getLong("Home"));
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
}
