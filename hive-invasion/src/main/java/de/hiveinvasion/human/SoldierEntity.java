package de.hiveinvasion.human;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.entity.Alien;
import de.hiveinvasion.military.MilitaryNet;
import de.hiveinvasion.military.MilitaryState;
import de.hiveinvasion.registry.ModEffects;
import de.hiveinvasion.registry.ModItems;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Soldat mit Sturmgewehr. Verteidigt Stützpunkt und Stadt, schießt auf Aliens und Infizierte
 * kurz vor dem Ausbruch. Gehört er zum Trupp eines Spielers, gehorcht er dessen Funkbefehlen.
 */
public class SoldierEntity extends HumanEntity {
    private static final TrackedData<Boolean> AIMING = DataTracker.registerData(SoldierEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final float BASE_DAMAGE = 6.0f;

    @Nullable private UUID leader;
    private SoldierOrder order = SoldierOrder.NONE;
    @Nullable private BlockPos orderPos;
    @Nullable private UUID orderTarget;
    private boolean guard;

    public SoldierEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 32.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, BASE_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    @Override public int variantCount() { return 4; }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(AIMING, false);
    }

    public boolean isAiming() { return dataTracker.get(AIMING); }
    void setAiming(boolean a) { dataTracker.set(AIMING, a); }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new ShootGoal(this));
        goalSelector.add(2, new OrderGoal(this));
        goalSelector.add(4, new StayNearHomeGoal(this, 40, 1.0));
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.6) {
            @Override public boolean canStart() { return !guard && order == SoldierOrder.NONE && super.canStart(); }
        });
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10f));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this, SoldierEntity.class).setGroupRevenge());
        targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isEnemy));
    }

    /** Wen greift der Soldat von sich aus an? */
    public boolean isEnemy(LivingEntity e) {
        if (e instanceof Alien) return true;
        StatusEffectInstance p = e.getStatusEffect(ModEffects.PARASITE);
        if (p != null && p.getDuration() < 600 && !(e instanceof PlayerEntity)) return true;
        if (e instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator()
                && getWorld() instanceof ServerWorld sw && home != null) {
            return MilitaryState.get(sw).isHostile(player.getUuid(), home);
        }
        return false;
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof SoldierEntity) return false;
        if (target instanceof PlayerEntity p && leader != null && leader.equals(p.getUuid())) return false;
        return super.canTarget(target);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason, @Nullable EntityData data) {
        EntityData d = super.initialize(world, difficulty, reason, data);
        equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.RIFLE));
        setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.35f);
        return d;
    }

    @Override
    protected net.minecraft.util.ActionResult interactMob(PlayerEntity player, net.minecraft.util.Hand hand) {
        if (getWorld() instanceof ServerWorld sw && player instanceof net.minecraft.server.network.ServerPlayerEntity sp && getTarget() != player) {
            if (leader != null && leader.equals(player.getUuid())) {
                sp.sendMessage(net.minecraft.text.Text.translatable("dialog.hiveinvasion.squad_member"), true);
            } else {
                getLookControl().lookAt(player);
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp,
                        new MilitaryNet.OpenDialog(getId(), MilitaryState.get(sw).isMember(player.getUuid())));
            }
        }
        return net.minecraft.util.ActionResult.success(getWorld().isClient);
    }

    // ---------------------------------------------------------------- Befehle
    public void command(@Nullable UUID leader, SoldierOrder order, @Nullable BlockPos pos, @Nullable UUID target) {
        if (leader != null) this.leader = leader;
        this.order = order; this.orderPos = pos; this.orderTarget = target;
        getNavigation().stop();
        if (order != SoldierOrder.ATTACK) setTarget(null);
    }

    @Nullable public UUID getLeader() { return leader; }
    public void setLeader(@Nullable UUID leader) { this.leader = leader; }
    public SoldierOrder getOrder() { return order; }
    public void setGuard(boolean guard) { this.guard = guard; }
    public boolean isGuard() { return guard; }

    /** Führt den aktuellen Befehl aus. */
    static class OrderGoal extends Goal {
        private final SoldierEntity s;
        private int repath;

        OrderGoal(SoldierEntity s) { this.s = s; setControls(EnumSet.of(Control.MOVE)); }

        @Override public boolean canStart() { return s.order != SoldierOrder.NONE && (s.getTarget() == null || s.order == SoldierOrder.ATTACK); }
        @Override public boolean shouldContinue() { return canStart(); }

        @Override
        public void tick() {
            if (--repath > 0) return;
            repath = 10;
            ServerWorld world = (ServerWorld) s.getWorld();
            switch (s.order) {
                case FOLLOW -> {
                    PlayerEntity p = s.leader == null ? null : world.getPlayerByUuid(s.leader);
                    if (p == null) return;
                    double d = s.squaredDistanceTo(p);
                    if (d > 1600) s.requestTeleport(p.getX() + s.random.nextInt(5) - 2, p.getY(), p.getZ() + s.random.nextInt(5) - 2);
                    else if (d > 25) Navigation.moveToward(s, p.getPos(), 1.15);
                    else s.getNavigation().stop();
                }
                case MOVE -> {
                    if (s.orderPos != null && !s.orderPos.isWithinDistance(s.getPos(), 3))
                        Navigation.moveToward(s, Vec3d.ofBottomCenter(s.orderPos), 1.1);
                }
                case RETURN -> {
                    if (s.home == null) { s.order = SoldierOrder.NONE; return; }
                    if (s.home.isWithinDistance(s.getPos(), 6)) { s.order = SoldierOrder.NONE; return; }
                    Navigation.moveToward(s, Vec3d.ofBottomCenter(s.home), 1.1);
                }
                case ATTACK -> {
                    if (s.orderTarget != null && world.getEntity(s.orderTarget) instanceof LivingEntity t && t.isAlive()) {
                        s.setTarget(t);
                        if (s.squaredDistanceTo(t) > 400) Navigation.moveToward(s, t.getPos(), 1.15);
                    } else if (s.orderPos != null && !s.orderPos.isWithinDistance(s.getPos(), 4)) {
                        Navigation.moveToward(s, Vec3d.ofBottomCenter(s.orderPos), 1.15);
                    } else if (s.getTarget() == null) {
                        s.order = SoldierOrder.FOLLOW;
                    }
                }
                default -> {}
            }
        }
    }

    /** Zielen und in kurzen Feuerstößen schießen. */
    static class ShootGoal extends Goal {
        private final SoldierEntity s;
        private int cooldown, burst;

        ShootGoal(SoldierEntity s) { this.s = s; setControls(EnumSet.of(Control.LOOK, Control.MOVE)); }

        @Override public boolean canStart() { LivingEntity t = s.getTarget(); return t != null && t.isAlive() && s.squaredDistanceTo(t) < 48 * 48; }
        @Override public boolean shouldContinue() { return canStart(); }
        @Override public void stop() { s.setAiming(false); burst = 0; }

        @Override
        public void tick() {
            LivingEntity t = s.getTarget();
            if (t == null) return;
            s.getLookControl().lookAt(t, 30f, 30f);
            boolean sees = s.getVisibilityCache().canSee(t);
            double dist = s.squaredDistanceTo(t);
            if (!sees || dist > 28 * 28) Navigation.moveToward(s, t.getPos(), 1.1);
            else if (dist < 5 * 5) {   // zu nah: zurückweichen
                Vec3d away = s.getPos().subtract(t.getPos()).normalize().multiply(4);
                s.getNavigation().startMovingTo(s.getX() + away.x, s.getY(), s.getZ() + away.z, 1.2);
            } else s.getNavigation().stop();
            s.setAiming(sees);
            if (!sees || --cooldown > 0) return;
            Vec3d dir = t.getPos().add(0, t.getHeight() * 0.55, 0).subtract(s.getEyePos()).normalize();
            Firearms.shoot((ServerWorld) s.getWorld(), s, dir, 0.035f, BASE_DAMAGE * 0.8f, 48);
            if (++burst >= 3) { burst = 0; cooldown = 14 + s.random.nextInt(10); } else cooldown = 3;
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (leader != null) nbt.putUuid("Leader", leader);
        nbt.putString("Order", order.name());
        if (orderPos != null) nbt.putLong("OrderPos", orderPos.asLong());
        nbt.putBoolean("Guard", guard);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Leader")) leader = nbt.getUuid("Leader");
        try { order = SoldierOrder.valueOf(nbt.getString("Order")); } catch (Exception e) { order = SoldierOrder.NONE; }
        if (nbt.contains("OrderPos")) orderPos = BlockPos.fromLong(nbt.getLong("OrderPos"));
        guard = nbt.getBoolean("Guard");
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_PLAYER_DEATH; }
}
