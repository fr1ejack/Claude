package de.hiveinvasion.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Leiche: bleibt nach dem Tod liegen, hat Schwerkraft, lässt sich schieben und
 * wegschlagen, und enthält die Beute des Toten (Rechtsklick öffnet sie).
 * Schleichen + Schlagen beseitigt die Leiche (Inhalt fällt heraus).
 */
public class CorpseEntity extends Entity {
    private static final TrackedData<NbtCompound> BODY = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final TrackedData<String> PLAYER_NAME = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Optional<UUID>> PLAYER_ID = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> AGE = DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** So lange (Ticks) sieht man noch die normale Umfall-Animation, bevor die Leiche erscheint. */
    public static final int FADE_IN = 19;
    private static final int COLLECT_TICKS = 4;

    private final SimpleInventory inventory = new SimpleInventory(54);
    private int lifetime = 20 * 60 * 30;
    /** Nur auf dem Client: zwischengespeichertes Modell-Objekt für die Darstellung. */
    public Object renderCache;

    public CorpseEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    /** Erzeugt eine Leiche für das gestorbene Lebewesen. */
    public static CorpseEntity of(EntityType<CorpseEntity> type, LivingEntity dead) {
        CorpseEntity c = new CorpseEntity(type, dead.getWorld());
        c.refreshPositionAndAngles(dead.getX(), dead.getY(), dead.getZ(), dead.getBodyYaw(), 0);
        c.setVelocity(dead.getVelocity().multiply(1.2).add(0, 0.1, 0));
        if (dead instanceof PlayerEntity p) {
            c.dataTracker.set(PLAYER_NAME, p.getGameProfile().getName());
            c.dataTracker.set(PLAYER_ID, Optional.of(p.getUuid()));
            c.lifetime = 20 * 60 * 60;
        } else {
            NbtCompound nbt = new NbtCompound();
            dead.saveSelfNbt(nbt);
            for (String k : List.of("ArmorItems", "HandItems", "Inventory", "Offers", "Brain", "Attributes",
                    "active_effects", "Passengers", "Gossips", "leash", "Owner", "UUID", "Pos", "Motion",
                    "CustomName", "body_armor_item", "SaddleItem", "ArmorDropChances", "HandDropChances",
                    "DeathTime", "HurtTime", "HurtByTimestamp", "Fire", "Health"))
                nbt.remove(k);
            c.dataTracker.set(BODY, nbt);
            c.lifetime = 20 * 60 * 15;
        }
        return c;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(BODY, new NbtCompound());
        builder.add(PLAYER_NAME, "");
        builder.add(PLAYER_ID, Optional.empty());
        builder.add(AGE, 0);
    }

    public NbtCompound getBody() { return dataTracker.get(BODY); }
    public String getPlayerName() { return dataTracker.get(PLAYER_NAME); }
    public Optional<UUID> getPlayerId() { return dataTracker.get(PLAYER_ID); }
    public int getCorpseAge() { return dataTracker.get(AGE); }
    public boolean isPlayerCorpse() { return getPlayerId().isPresent(); }

    @Override
    public void tick() {
        super.tick();
        // Physik: Schwerkraft, Reibung, Wasser.
        Vec3d v = getVelocity();
        if (!hasNoGravity()) v = v.add(0, isTouchingWater() ? -0.01 : -0.08, 0);
        setVelocity(v);
        move(MovementType.SELF, getVelocity());
        double friction = isOnGround() ? 0.55 : 0.98;
        setVelocity(getVelocity().multiply(friction, 0.98, friction));
        if (isTouchingWater()) setVelocity(getVelocity().multiply(0.8));

        if (getWorld() instanceof ServerWorld world) {
            int age = getCorpseAge() + 1;
            dataTracker.set(AGE, age);
            if (age <= COLLECT_TICKS + FADE_IN) collectDrops(world);
            if (age > lifetime || (inventory.isEmpty() && age > 20 * 60 * 5)) discard();
        }
    }

    /** Sammelt die Gegenstände ein, die der Tote gerade fallen gelassen hat. */
    private void collectDrops(ServerWorld world) {
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, getBoundingBox().expand(3.5, 2.0, 3.5),
                i -> i.getItemAge() < 40 && i.isAlive())) {
            ItemStack rest = inventory.addStack(item.getStack().copy());
            if (rest.isEmpty()) item.discard(); else item.setStack(rest);
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (getCorpseAge() < FADE_IN) return ActionResult.PASS;
        if (!getWorld().isClient) {
            Text title = isPlayerCorpse()
                    ? Text.translatable("container.hiveinvasion.corpse_of", getPlayerName())
                    : Text.translatable("container.hiveinvasion.corpse");
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> GenericContainerScreenHandler.createGeneric9x6(syncId, inv, inventory), title));
            getWorld().playSound(null, getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.8f, 0.7f);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || getWorld().isClient) return false;
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity p && p.isSneaking()) {
            ItemScatterer.spawn(getWorld(), this, inventory);
            getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1, 0.8f);
            discard();
            return true;
        }
        // Wegschleudern in Schlagrichtung bzw. von der Explosion weg.
        Vec3d from = source.getPosition() != null ? source.getPosition() : (attacker != null ? attacker.getPos() : null);
        if (from != null) {
            Vec3d push = getPos().subtract(from).multiply(1, 0, 1).normalize().multiply(Math.min(1.5, 0.2 + amount * 0.06));
            addVelocity(push.x, 0.15 + Math.min(0.6, amount * 0.03), push.z);
            velocityModified = true;
        }
        return true;
    }

    @Override public boolean canHit() { return !isRemoved(); }
    @Override public boolean isPushable() { return true; }
    @Override public boolean isCollidable() { return false; }

    @Override
    public void pushAwayFrom(Entity entity) {
        if (entity instanceof CorpseEntity) return;
        super.pushAwayFrom(entity);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("Body", getBody());
        nbt.putString("PlayerName", getPlayerName());
        getPlayerId().ifPresent(id -> nbt.putUuid("PlayerId", id));
        nbt.putInt("CorpseAge", getCorpseAge());
        nbt.putInt("Lifetime", lifetime);
        nbt.put("Items", inventory.toNbtList(getRegistryManager()));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(BODY, nbt.getCompound("Body"));
        dataTracker.set(PLAYER_NAME, nbt.getString("PlayerName"));
        if (nbt.containsUuid("PlayerId")) dataTracker.set(PLAYER_ID, Optional.of(nbt.getUuid("PlayerId")));
        dataTracker.set(AGE, Math.max(FADE_IN + COLLECT_TICKS + 1, nbt.getInt("CorpseAge")));
        if (nbt.contains("Lifetime")) lifetime = nbt.getInt("Lifetime");
        inventory.readNbtList(nbt.getList("Items", NbtElement.COMPOUND_TYPE), getRegistryManager());
    }
}
