package de.hiveinvasion.hive;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.entity.Alien;
import de.hiveinvasion.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Die Königin: riesig, langsam, legt in ihrer Kammer ständig neue Eier. Endgegner. */
public class QueenEntity extends HostileEntity implements Alien {
    private static final TrackedData<Integer> LAYING = DataTracker.registerData(QueenEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final int LAY_TIME = 60;
    private final ServerBossBar bossBar = new ServerBossBar(net.minecraft.text.Text.translatable("entity.hiveinvasion.queen"),
            BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
    @Nullable private BlockPos home;
    private int layCooldown = 400;

    public QueenEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 200;
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 320.0 * HiveInvasion.ALIEN_STRENGTH)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0 * HiveInvasion.ALIEN_STRENGTH)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0);
    }

    public void setHive(HiveBuilder.Layout l) { this.home = l.queen; }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LAYING, 0);
    }

    public int getLaying() { return dataTracker.get(LAYING); }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true, Alien::isPrey));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        bossBar.setPercent(getHealth() / getMaxHealth());
        // Leine: die Königin verlässt ihre Kammer nicht
        if (home != null) {
            double d = home.getSquaredDistance(getPos());
            if (d > 14 * 14) setTarget(null);
            if (d > 22 * 22) refreshPositionAndAngles(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, getYaw(), 0);
            else if (d > 8 * 8 && getTarget() == null && age % 20 == 0)
                getNavigation().startMovingTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
        }
        int laying = getLaying();
        if (laying > 0) {
            dataTracker.set(LAYING, laying - 1);
            if (laying == 1) layEgg(world);
        } else if (--layCooldown <= 0 && getTarget() == null) {
            dataTracker.set(LAYING, LAY_TIME);
            layCooldown = 500 + random.nextInt(400);
            world.playSound(null, getBlockPos(), SoundEvents.BLOCK_SNIFFER_EGG_PLOP, SoundCategory.HOSTILE, 2f, 0.5f);
        }
    }

    private void layEgg(ServerWorld world) {
        int pods = 0;
        for (BlockPos p : BlockPos.iterate(getBlockPos().add(-7, -2, -7), getBlockPos().add(7, 2, 7)))
            if (world.getBlockState(p).isOf(ModBlocks.BROOD_POD)) pods++;
        if (pods >= 10) return;
        for (int i = 0; i < 12; i++) {
            BlockPos p = getBlockPos().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos q = p.up(dy);
                BlockState below = world.getBlockState(q.down());
                if (world.getBlockState(q).isAir() && below.isSideSolidFullSquare(world, q.down(), net.minecraft.util.math.Direction.UP)
                        && q.getSquaredDistance(getBlockPos()) > 4) {
                    world.setBlockState(q, ModBlocks.BROOD_POD.getDefaultState());
                    world.playSound(null, q, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.HOSTILE, 1.5f, 0.5f);
                    return;
                }
            }
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) { super.onStartedTrackingBy(player); bossBar.addPlayer(player); }
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) { super.onStoppedTrackingBy(player); bossBar.removePlayer(player); }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt) Alien.splashAcid(this, source);
        return hurt;
    }

    @Override public boolean cannotDespawn() { return true; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ENDER_DRAGON_GROWL; }
    @Override public float getSoundPitch() { return 0.6f; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (home != null) nbt.putLong("Home", home.asLong());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Home")) home = BlockPos.fromLong(nbt.getLong("Home"));
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
