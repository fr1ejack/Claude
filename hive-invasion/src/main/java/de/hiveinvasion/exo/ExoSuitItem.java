package de.hiveinvasion.exo;

import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/** Stellt einen offenen Exo-Anzug auf. Er schaut dabei zum Spieler. */
public class ExoSuitItem extends Item {
    public ExoSuitItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;
        BlockPos pos = ctx.getBlockPos().offset(ctx.getSide());
        ExoSuitEntity suit = ModEntities.EXO_SUIT.create(world);
        if (suit == null) return ActionResult.FAIL;
        PlayerEntity player = ctx.getPlayer();
        float yaw = player != null ? player.getYaw() + 180f : 0f;
        suit.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        suit.setBodyYaw(yaw);
        suit.setHeadYaw(yaw);
        if (!world.isSpaceEmpty(suit)) return ActionResult.FAIL;
        world.spawnEntity(suit);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.6f, 0.8f);
        ctx.getStack().decrementUnlessCreative(1, player);
        if (player != null && !player.getInventory().contains(new ItemStack(ModItems.EXO_TRIGGER))) {
            player.giveItemStack(new ItemStack(ModItems.EXO_TRIGGER));
        }
        return ActionResult.CONSUME;
    }
}
