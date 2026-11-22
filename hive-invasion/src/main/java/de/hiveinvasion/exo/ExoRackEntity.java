package de.hiveinvasion.exo;

import de.hiveinvasion.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Montagegestell für den Exo-Anzug. Ein leerer Anzug in der Nähe dockt automatisch an. */
public class ExoRackEntity extends Entity {
    public ExoRackEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (!hasNoGravity()) {
            setVelocity(getVelocity().add(0, -0.08, 0));
            move(MovementType.SELF, getVelocity());
            setVelocity(getVelocity().multiply(0, 0.98, 0));
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient || isInvulnerableTo(source)) return false;
        if (source.getAttacker() instanceof PlayerEntity p && p.isSneaking()) {
            if (!p.isCreative()) dropStack(new ItemStack(ModItems.EXO_RACK));
            getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.BLOCKS, 0.6f, 1.2f);
            discard();
            return true;
        }
        return false;
    }

    @Override public boolean canHit() { return !isRemoved(); }
    @Override public boolean isCollidable() { return false; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
