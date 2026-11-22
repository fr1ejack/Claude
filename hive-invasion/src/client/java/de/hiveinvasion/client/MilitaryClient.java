package de.hiveinvasion.client;

import de.hiveinvasion.client.screen.GuardDialogScreen;
import de.hiveinvasion.client.screen.RadioScreen;
import de.hiveinvasion.military.MilitaryNet;
import de.hiveinvasion.registry.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class MilitaryClient {
    private MilitaryClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MilitaryNet.OpenDialog.ID, (p, ctx) ->
                ctx.client().setScreen(new GuardDialogScreen(p.soldierId(), p.member())));
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);
            if (!world.isClient || !stack.isOf(ModItems.RADIO)) return TypedActionResult.pass(stack);
            MinecraftClient mc = MinecraftClient.getInstance();
            double range = 128;
            Vec3d eye = player.getEyePos(), look = player.getRotationVec(1f), end = eye.add(look.multiply(range));
            HitResult block = player.raycast(range, 1f, false);
            Vec3d limit = block.getType() == HitResult.Type.MISS ? end : block.getPos();
            EntityHitResult ent = ProjectileUtil.raycast(player, eye, limit, player.getBoundingBox().stretch(look.multiply(range)).expand(1),
                    e -> e instanceof LivingEntity && e != player, range * range);
            BlockPos pos; int id = -1; Text name;
            if (ent != null) {
                Entity e = ent.getEntity();
                pos = e.getBlockPos(); id = e.getId(); name = e.getDisplayName();
            } else if (block instanceof BlockHitResult bh && block.getType() == HitResult.Type.BLOCK) {
                pos = bh.getBlockPos().up(); name = Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ());
            } else {
                pos = BlockPos.ofFloored(end); name = Text.translatable("radio.hiveinvasion.far");
            }
            mc.setScreen(new RadioScreen(pos, id, name));
            return TypedActionResult.success(stack);
        });
    }
}
