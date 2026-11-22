package de.hiveinvasion.exo;

import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Exo-Anzug. Steht offen herum, bis sich ein Spieler mit dem Rücken davor stellt und
 * den Auslöser drückt. Dann schließt er sich Teil für Teil um den Spieler (Beine, Brust,
 * Arme, Helm). Geschlossen: 1,5-fache Größe, Plasmakanone (Linksklick), Energieschild
 * (Rechtsklick halten), Sprungdüsen (Leertaste halten). Schleichen = aussteigen.
 */
public class ExoSuitEntity extends MobEntity {
    public static final int ASSEMBLE_TICKS = 70;
    public static final float MAX_ENERGY = 100f;

    private static final TrackedData<Integer> PROGRESS = DataTracker.registerData(ExoSuitEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> ENERGY = DataTracker.registerData(ExoSuitEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SHIELD = DataTracker.registerData(ExoSuitEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BOOST = DataTracker.registerData(ExoSuitEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> ASSEMBLER = DataTracker.registerData(ExoSuitEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private boolean inFire, inShield, inBoost;
    private int fireCooldown;
    /** Nur Client: Fortschritt im vorigen Tick, für flüssige Animation. */
    public int lastProgress;

    public ExoSuitEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 150.0)
                .add(EntityAttributes.GENERIC_ARMOR, 14.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.1)
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 14.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PROGRESS, 0);
        builder.add(ENERGY, MAX_ENERGY);
        builder.add(SHIELD, false);
        builder.add(BOOST, false);
        builder.add(ASSEMBLER, Optional.empty());
    }

    public int getProgress() { return dataTracker.get(PROGRESS); }
    public float getEnergy() { return dataTracker.get(ENERGY); }
    public boolean isShieldActive() { return dataTracker.get(SHIELD); }
    public boolean isBoosting() { return dataTracker.get(BOOST); }
    public Optional<UUID> getAssembler() { return dataTracker.get(ASSEMBLER); }
    public boolean isClosed() { return getProgress() >= ASSEMBLE_TICKS; }

    /** 0 = ganz offen, 1 = ganz geschlossen (für die Animation). */
    public float getAssembly(float tickDelta) {
        return MathHelper.lerp(tickDelta, (float) lastProgress, (float) getProgress()) / ASSEMBLE_TICKS;
    }

    public void setInput(boolean fire, boolean shield, boolean boost) {
        this.inFire = fire; this.inShield = shield; this.inBoost = boost;
    }

    // ------------------------------------------------------------------ Anlegen

    /** Steht der Spieler mit dem Rücken direkt vor dem Anzug? */
    public boolean isBackedIn(PlayerEntity player) {
        Vec3d forward = Vec3d.fromPolar(0, getYaw());
        Vec3d toPlayer = player.getPos().subtract(getPos()).multiply(1, 0, 1);
        if (toPlayer.length() > 1.8) return false;
        Vec3d facing = Vec3d.fromPolar(0, player.getYaw());
        boolean inFront = toPlayer.lengthSquared() < 0.09 || toPlayer.normalize().dotProduct(forward) > 0.3;
        boolean backTurned = facing.dotProduct(forward) > 0.5;   // schaut in dieselbe Richtung wie der Anzug
        return inFront && backTurned;
    }

    public boolean canBeEntered() {
        return !hasPassengers() && getAssembler().isEmpty() && isAlive();
    }

    /** Startet die Anlege-Sequenz. */
    public void beginAssembly(ServerPlayerEntity player) {
        dataTracker.set(ASSEMBLER, Optional.of(player.getUuid()));
        dataTracker.set(PROGRESS, 0);
        player.networkHandler.requestTeleport(getX(), getY(), getZ(), getYaw(), 0);
        getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 1, 0.6f);
    }

    private void tickAssembly(ServerWorld world) {
        UUID id = getAssembler().orElse(null);
        if (id == null) {
            // Leer: langsam wieder öffnen.
            if (!hasPassengers() && getProgress() > 0) dataTracker.set(PROGRESS, Math.max(0, getProgress() - 3));
            return;
        }
        PlayerEntity player = world.getPlayerByUuid(id);
        if (player == null || !player.isAlive() || player.squaredDistanceTo(this) > 4.0) {
            dataTracker.set(ASSEMBLER, Optional.empty());
            return;
        }
        // Spieler festhalten, während sich der Anzug schließt.
        if (player instanceof ServerPlayerEntity sp && player.squaredDistanceTo(this) > 0.02) {
            sp.networkHandler.requestTeleport(getX(), getY(), getZ(), getYaw(), sp.getPitch());
        }
        player.setVelocity(Vec3d.ZERO);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 9, false, false, false));

        int p = getProgress() + 1;
        dataTracker.set(PROGRESS, p);
        SoundEvent sound = switch (p) {
            case 12 -> SoundEvents.BLOCK_IRON_DOOR_CLOSE;       // Beine
            case 28 -> SoundEvents.BLOCK_ANVIL_LAND;            // Brust
            case 44 -> SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(); // Arme
            case 58 -> SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE;   // Helm
            case ASSEMBLE_TICKS -> SoundEvents.BLOCK_BEACON_ACTIVATE;
            default -> null;
        };
        if (sound != null) {
            world.playSound(null, getBlockPos(), sound, SoundCategory.PLAYERS, p == 28 ? 0.35f : 1f, p == ASSEMBLE_TICKS ? 1.6f : 0.8f);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2 + p / 50.0, getZ(), 12, 0.4, 0.3, 0.4, 0.1);
        }
        if (p >= ASSEMBLE_TICKS) {
            dataTracker.set(ASSEMBLER, Optional.empty());
            player.startRiding(this, true);
            player.sendMessage(Text.translatable("message.hiveinvasion.exo_online").formatted(Formatting.AQUA), true);
        }
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void tick() {
        if (getWorld().isClient) {
            lastProgress = getProgress();
        }
        super.tick();
        if (getWorld() instanceof ServerWorld world) {
            tickAssembly(world);
            tickSystems(world);
        } else if (isBoosting()) {
            Vec3d back = Vec3d.fromPolar(0, getYaw()).multiply(-0.45);
            for (int side = -1; side <= 1; side += 2) {
                Vec3d right = Vec3d.fromPolar(0, getYaw() + 90).multiply(0.25 * side);
                double x = getX() + back.x + right.x, z = getZ() + back.z + right.z, y = getY() + 1.15;
                getWorld().addParticle(ParticleTypes.FLAME, x, y, z, 0, -0.25, 0);
                getWorld().addParticle(ParticleTypes.SMOKE, x, y - 0.2, z, 0, -0.1, 0);
            }
        }
    }

    private void tickSystems(ServerWorld world) {
        boolean manned = isClosed() && getFirstPassenger() instanceof PlayerEntity;
        if (!manned) inFire = inShield = inBoost = false;
        float energy = getEnergy();

        boolean shield = inShield && energy > 1;
        boolean boost = inBoost && energy > 1;
        dataTracker.set(SHIELD, shield);
        dataTracker.set(BOOST, boost && !isOnGround());

        if (shield) energy -= 0.45f;
        if (boost && !isOnGround()) energy -= 1.1f;
        if (!shield && !(boost && !isOnGround()) && fireCooldown <= 0) energy += 0.6f;

        if (fireCooldown > 0) fireCooldown--;
        if (inFire && manned && fireCooldown <= 0 && energy >= 3 && !shield) {
            shoot(world, (PlayerEntity) getFirstPassenger());
            energy -= 3;
            fireCooldown = 5;
        }
        if (shield) pushBackInFront(world);
        dataTracker.set(ENERGY, MathHelper.clamp(energy, 0, MAX_ENERGY));
    }

    private void shoot(ServerWorld world, PlayerEntity pilot) {
        Vec3d look = pilot.getRotationVec(1f);
        Vec3d right = Vec3d.fromPolar(0, getYaw() + 90).multiply(-0.75);
        Vec3d muzzle = new Vec3d(getX() + right.x + look.x * 0.9, getY() + 1.55 + look.y * 0.9, getZ() + right.z + look.z * 0.9);
        PlasmaBoltEntity bolt = new PlasmaBoltEntity(ModEntities.PLASMA_BOLT, world);
        bolt.setOwner(pilot);
        bolt.setPosition(muzzle);
        bolt.setVelocity(look.x, look.y, look.z, 3.2f, 0.6f);
        world.spawnEntity(bolt);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.8f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 0.5f, 1.4f);
    }

    /** Der Schild schiebt Gegner vor dem Anzug weg. */
    private void pushBackInFront(ServerWorld world) {
        Vec3d forward = Vec3d.fromPolar(0, getYaw());
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(1.6), e -> e != this && !hasPassenger(e))) {
            Vec3d to = e.getPos().subtract(getPos()).multiply(1, 0, 1);
            if (to.lengthSquared() > 0.01 && to.normalize().dotProduct(forward) > 0.5) {
                Vec3d push = to.normalize().multiply(0.35);
                e.addVelocity(push.x, 0.05, push.z);
                e.velocityModified = true;
            }
        }
    }

    // ------------------------------------------------------------------ Steuerung

    @Override
    public LivingEntity getControllingPassenger() {
        return isClosed() && getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    @Override
    protected void tickControlled(PlayerEntity pilot, Vec3d movementInput) {
        super.tickControlled(pilot, movementInput);
        setRotation(pilot.getYaw(), pilot.getPitch() * 0.5f);
        this.prevYaw = this.bodyYaw = this.headYaw = getYaw();
        // Sprungdüsen: werden dort berechnet, wo die Bewegung berechnet wird (beim Piloten).
        if (inBoost && getEnergy() > 1) {
            Vec3d v = getVelocity();
            setVelocity(v.x, Math.min(v.y + (isOnGround() ? 0.5 : 0.11), 0.45), v.z);
            this.fallDistance = 0;
        }
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity pilot, Vec3d movementInput) {
        float forward = pilot.forwardSpeed;
        if (forward < 0) forward *= 0.5f;
        return new Vec3d(pilot.sidewaysSpeed * 0.6f, 0, forward);
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity pilot) {
        float speed = (float) getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        return isBoosting() ? speed * 1.6f : speed;
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vec3d(0, 1.4, 0);
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        Vec3d front = Vec3d.fromPolar(0, getYaw()).multiply(1.3);
        return new Vec3d(getX() + front.x, getY(), getZ() + front.z);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!getWorld().isClient) {
            getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 1, 0.7f);
        }
    }

    // ------------------------------------------------------------------ Interaktion

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (hasPassengers()) return ActionResult.PASS;
        if (stack.isOf(Items.IRON_INGOT) && getHealth() < getMaxHealth()) {
            if (!getWorld().isClient) {
                heal(15);
                stack.decrementUnlessCreative(1, player);
                getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.6f, 1.4f);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (player.isSneaking() && canBeEntered()) {
            if (!getWorld().isClient) {
                ItemStack item = new ItemStack(ModItems.EXO_SUIT);
                if (!player.giveItemStack(item)) dropStack(item);
                discard();
            }
            return ActionResult.success(getWorld().isClient);
        }
        // Auslöseknopf am Anzug drücken: wer davor steht, wird umgedreht und tritt rückwärts hinein.
        if (canBeEntered()) {
            Vec3d toPlayer = player.getPos().subtract(getPos()).multiply(1, 0, 1);
            if (toPlayer.length() < 2.2 && toPlayer.normalize().dotProduct(Vec3d.fromPolar(0, getYaw())) > 0.2) {
                if (player instanceof ServerPlayerEntity sp) beginAssembly(sp);
                return ActionResult.success(getWorld().isClient);
            }
            if (!getWorld().isClient) {
                player.sendMessage(Text.translatable("message.hiveinvasion.exo_hint").formatted(Formatting.GRAY), true);
            }
        }
        return ActionResult.success(getWorld().isClient);
    }

    // ------------------------------------------------------------------ Schaden

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() != null && hasPassenger(source.getAttacker())) return false;
        if (isShieldActive() && source.getPosition() != null) {
            Vec3d to = source.getPosition().subtract(getPos()).multiply(1, 0, 1);
            if (to.lengthSquared() < 0.01 || to.normalize().dotProduct(Vec3d.fromPolar(0, getYaw())) > 0.2) {
                dataTracker.set(ENERGY, Math.max(0, getEnergy() - amount * 1.2f));
                getWorld().playSound(null, getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1, 1.5f);
                return false;
            }
        }
        boolean hurt = super.damage(source, amount);
        if (hurt && getFirstPassenger() instanceof PlayerEntity p && getHealth() < getMaxHealth() * 0.25f && age % 40 == 0) {
            p.sendMessage(Text.translatable("message.hiveinvasion.exo_critical").formatted(Formatting.RED), true);
        }
        return hurt;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        removeAllPassengers();
        if (getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.2, getZ(), 4, 0.5, 0.6, 0.5, 0);
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1, 1.2f);
            dropStack(new ItemStack(Items.IRON_INGOT, 6));
            dropStack(new ItemStack(Items.REDSTONE, 4));
        }
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void tickCramming() {}
    @Override public boolean cannotDespawn() { return true; }
    @Override protected boolean isDisallowedInPeaceful() { return false; }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        if (fallDistance > 3 && !getWorld().isClient) {
            getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.4f, 0.5f);
        }
        return super.handleFallDamage(fallDistance, damageMultiplier * 0.5f, source);
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_IRON_GOLEM_DEATH; }
    @Override protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP, 0.6f, 0.8f);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("Energy", getEnergy());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Energy")) dataTracker.set(ENERGY, nbt.getFloat("Energy"));
    }
}
