package de.hiveinvasion.human;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Sturmgewehr: Rechtsklick schießt (Sofort-Treffer mit Leuchtspur). */
public class RifleItem extends Item {
    public RifleItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld sw) {
            Firearms.shoot(sw, user, user.getRotationVec(1f), 0.012f, 6f, 64);
            user.getItemCooldownManager().set(this, 3);
            user.setPitch(user.getPitch() - 0.8f);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
