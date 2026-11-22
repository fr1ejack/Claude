package de.hiveinvasion.gore;

import de.hiveinvasion.fx.BloodFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Abgetrenntes Körperteil: fliegt, fällt, rollt aus, blutet und bleibt eine Weile liegen. */
public class SeveredLimbEntity extends Entity {
    private static final TrackedData<NbtCompound> BODY = DataTracker.registerData(SeveredLimbEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final TrackedData<Integer> LIMB = DataTracker.registerData(SeveredLimbEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BLOOD = DataTracker.registerData(SeveredLimbEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> PLAYER_NAME = DataTracker.registerData(SeveredLimbEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Optional<UUID>> PLAYER_ID = DataTracker.registerData(SeveredLimbEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private int life;
    private boolean landed;
    /** Nur Client: Darstellungsdaten. */
    public Object renderCache;

    public SeveredLimbEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public static SeveredLimbEntity of(EntityType<SeveredLimbEntity> type, LivingEntity from, Limb limb) {
        SeveredLimbEntity e = new SeveredLimbEntity(type, from.getWorld());
        double y = switch (limb) {
            case HEAD -> from.getEyeY();
            case RIGHT_ARM, LEFT_ARM -> from.getBodyY(0.62);
            default -> from.getBodyY(0.25);
        };
        Vec3d side = Vec3d.fromPolar(0, from.getBodyYaw() + 90).multiply(limb == Limb.LEFT_ARM || limb == Limb.LEFT_LEG ? -0.3 : 0.3);
        if (limb == Limb.HEAD) side = Vec3d.ZERO;
        e.refreshPositionAndAngles(from.getX() + side.x, y, from.getZ() + side.z, from.getBodyYaw(), 0);
        if (from instanceof PlayerEntity p) {
            e.dataTracker.set(PLAYER_NAME, p.getGameProfile().getName());
            e.dataTracker.set(PLAYER_ID, Optional.of(p.getUuid()));
        } else {
            NbtCompound nbt = new NbtCompound();
            from.saveSelfNbt(nbt);
            for (String k : List.of("ArmorItems", "HandItems", "Inventory", "Brain", "Attributes", "active_effects",
                    "Passengers", "UUID", "Pos", "Motion", "DeathTime", "HurtTime", "Health", "CustomName"))
                nbt.remove(k);
            e.dataTracker.set(BODY, nbt);
        }
        e.dataTracker.set(LIMB, limb.ordinal());
        e.dataTracker.set(BLOOD, BloodFx.kindOf(from).ordinal());
        return e;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(BODY, new NbtCompound());
        builder.add(LIMB, 0);
        builder.add(BLOOD, BloodFx.Kind.RED.ordinal());
        builder.add(PLAYER_NAME, "");
        builder.add(PLAYER_ID, Optional.empty());
    }

    public NbtCompound getBody() { return dataTracker.get(BODY); }
    public Limb getLimb() { return Limb.byIndex(dataTracker.get(LIMB)); }
    public BloodFx.Kind bloodKind() { return BloodFx.Kind.values()[dataTracker.get(BLOOD)]; }
    public Optional<UUID> getPlayerId() { return dataTracker.get(PLAYER_ID); }
    public String getPlayerName() { return dataTracker.get(PLAYER_NAME); }

    @Override
    public void tick() {
        super.tick();
        Vec3d v = getVelocity().add(0, isTouchingWater() ? -0.01 : -0.06, 0);
        setVelocity(v);
        boolean wasOnGround = isOnGround();
        move(MovementType.SELF, getVelocity());
        if (isOnGround()) {
            if (!wasOnGround && v.y < -0.15) setVelocity(getVelocity().x * 0.6, -v.y * 0.3, getVelocity().z * 0.6);  // abprallen
            else setVelocity(getVelocity().multiply(0.6, 0.98, 0.6));
        } else setVelocity(getVelocity().multiply(0.98));

        if (getWorld() instanceof ServerWorld world) {
            if (isOnGround() && !landed) {
                landed = true;
                BloodFx.puddles(world, getBlockPos(), bloodKind(), 0, 1);
            }
            if (++life > 20 * 60 * 5) discard();
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient || isInvulnerableTo(source)) return false;
        Vec3d from = source.getPosition();
        if (from != null) {
            Vec3d push = getPos().subtract(from).multiply(1, 0, 1).normalize().multiply(0.3 + amount * 0.04);
            addVelocity(push.x, 0.25, push.z);
            velocityModified = true;
        }
        return true;
    }

    @Override public boolean canHit() { return !isRemoved(); }
    @Override public boolean isPushable() { return true; }

    public int getLife() { return life; }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("Body", getBody());
        nbt.putInt("Limb", dataTracker.get(LIMB));
        nbt.putInt("Blood", dataTracker.get(BLOOD));
        nbt.putInt("Life", life);
        nbt.putString("PlayerName", getPlayerName());
        getPlayerId().ifPresent(id -> nbt.putUuid("PlayerId", id));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(BODY, nbt.getCompound("Body"));
        dataTracker.set(LIMB, nbt.getInt("Limb"));
        dataTracker.set(BLOOD, nbt.getInt("Blood"));
        life = nbt.getInt("Life");
        landed = true;
        dataTracker.set(PLAYER_NAME, nbt.getString("PlayerName"));
        if (nbt.containsUuid("PlayerId")) dataTracker.set(PLAYER_ID, Optional.of(nbt.getUuid("PlayerId")));
    }
}
