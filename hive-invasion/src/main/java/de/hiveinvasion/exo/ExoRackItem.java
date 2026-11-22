package de.hiveinvasion.exo;

import de.hiveinvasion.registry.ModEntities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/** Stellt ein leeres Anzug-Gestell auf (schaut zum Spieler). */
public class ExoRackItem extends Item {
    public ExoRackItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;
        BlockPos pos = ctx.getBlockPos().offset(ctx.getSide());
        ExoRackEntity rack = ModEntities.EXO_RACK.create(world);
        if (rack == null) return ActionResult.FAIL;
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYaw() + 180f : 0f;
        rack.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        world.spawnEntity(rack);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.6f, 1.0f);
        ctx.getStack().decrementUnlessCreative(1, ctx.getPlayer());
        return ActionResult.CONSUME;
    }
}
