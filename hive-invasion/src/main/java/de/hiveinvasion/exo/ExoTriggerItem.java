package de.hiveinvasion.exo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Comparator;

/**
 * Auslöser: Stell dich mit dem Rücken vor deinen Exo-Anzug und drücke Rechtsklick –
 * der Anzug schließt sich um dich.
 */
public class ExoTriggerItem extends Item {
    public ExoTriggerItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        ExoSuitEntity suit = world.getEntitiesByClass(ExoSuitEntity.class, user.getBoundingBox().expand(2.5),
                        s -> s.canBeEntered() && s.isBackedIn(user))
                .stream().min(Comparator.comparingDouble(s -> s.squaredDistanceTo(user))).orElse(null);
        if (suit == null) {
            user.sendMessage(Text.translatable("message.hiveinvasion.exo_hint").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        suit.beginAssembly((ServerPlayerEntity) user);
        return TypedActionResult.success(stack);
    }
}
