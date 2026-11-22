package de.hiveinvasion.military;

import de.hiveinvasion.HiveInvasion;
import de.hiveinvasion.human.SoldierEntity;
import de.hiveinvasion.human.SoldierOrder;
import de.hiveinvasion.registry.ModEntities;
import de.hiveinvasion.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Gespräch mit der Wache und Funkbefehle. */
public final class MilitaryNet {
    private MilitaryNet() {}

    /** Server → Client: Gespräch öffnen. */
    public record OpenDialog(int soldierId, boolean member) implements CustomPayload {
        public static final Id<OpenDialog> ID = new Id<>(HiveInvasion.id("open_dialog"));
        public static final PacketCodec<RegistryByteBuf, OpenDialog> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, OpenDialog::soldierId, PacketCodecs.BOOL, OpenDialog::member, OpenDialog::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Client → Server: Antwort im Gespräch (0 = beitreten, 1 = angreifen, 2 = Verstärkung, 3 = gehen). */
    public record DialogChoice(int soldierId, int choice) implements CustomPayload {
        public static final Id<DialogChoice> ID = new Id<>(HiveInvasion.id("dialog_choice"));
        public static final PacketCodec<RegistryByteBuf, DialogChoice> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, DialogChoice::soldierId, PacketCodecs.VAR_INT, DialogChoice::choice, DialogChoice::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Client → Server: Funkbefehl. */
    public record RadioCommand(int order, BlockPos pos, int entityId) implements CustomPayload {
        public static final Id<RadioCommand> ID = new Id<>(HiveInvasion.id("radio"));
        public static final PacketCodec<RegistryByteBuf, RadioCommand> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, RadioCommand::order, BlockPos.PACKET_CODEC, RadioCommand::pos,
                PacketCodecs.VAR_INT, RadioCommand::entityId, RadioCommand::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenDialog.ID, OpenDialog.CODEC);
        PayloadTypeRegistry.playC2S().register(DialogChoice.ID, DialogChoice.CODEC);
        PayloadTypeRegistry.playC2S().register(RadioCommand.ID, RadioCommand.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DialogChoice.ID, (p, ctx) -> choice(ctx.player(), p));
        ServerPlayNetworking.registerGlobalReceiver(RadioCommand.ID, (p, ctx) -> radio(ctx.player(), p));
    }

    private static void choice(ServerPlayerEntity player, DialogChoice c) {
        ServerWorld world = player.getServerWorld();
        if (!(world.getEntityById(c.soldierId()) instanceof SoldierEntity guard) || guard.squaredDistanceTo(player) > 64) return;
        MilitaryState state = MilitaryState.get(world);
        BlockPos base = guard.getHome() != null ? guard.getHome() : guard.getBlockPos();
        switch (c.choice()) {
            case 0 -> { // beitreten
                if (state.isHostile(player.getUuid(), base)) {
                    player.sendMessage(Text.translatable("dialog.hiveinvasion.refused").formatted(Formatting.RED), false);
                    return;
                }
                state.join(player.getUuid());
                spawnSquad(world, player, guard, 10);
                if (!player.getInventory().contains(new ItemStack(ModItems.RADIO))) player.giveItemStack(new ItemStack(ModItems.RADIO));
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.hiveinvasion.joined").formatted(Formatting.GOLD)));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("title.hiveinvasion.squad")));
                world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
            }
            case 1 -> { // angreifen
                state.setHostile(player.getUuid(), base);
                for (SoldierEntity s : world.getEntitiesByClass(SoldierEntity.class, guard.getBoundingBox().expand(80), s -> s.getLeader() == null)) {
                    s.setTarget(player);
                }
                world.playSound(null, guard.getBlockPos(), SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 8f, 1.2f);
                player.sendMessage(Text.translatable("dialog.hiveinvasion.hostile").formatted(Formatting.DARK_RED), false);
            }
            case 2 -> { // Verstärkung für Mitglieder
                if (!state.isMember(player.getUuid())) return;
                long count = world.getEntitiesByClass(SoldierEntity.class, player.getBoundingBox().expand(200),
                        s -> player.getUuid().equals(s.getLeader())).size();
                if (count < 10) spawnSquad(world, player, guard, (int) (10 - count));
                if (!player.getInventory().contains(new ItemStack(ModItems.RADIO))) player.giveItemStack(new ItemStack(ModItems.RADIO));
            }
            default -> {}
        }
    }

    private static void spawnSquad(ServerWorld world, ServerPlayerEntity player, SoldierEntity guard, int n) {
        BlockPos base = guard.getHome() != null ? guard.getHome() : guard.getBlockPos();
        for (int i = 0; i < n; i++) {
            SoldierEntity s = ModEntities.SOLDIER.create(world);
            if (s == null) continue;
            s.refreshPositionAndAngles(guard.getX() + world.random.nextInt(5) - 2 + 0.5, guard.getY(), guard.getZ() + world.random.nextInt(5) - 2 + 0.5,
                    world.random.nextFloat() * 360, 0);
            s.setHome(base);
            s.initialize(world, world.getLocalDifficulty(s.getBlockPos()), SpawnReason.EVENT, null);
            s.setPersistent();
            s.command(player.getUuid(), SoldierOrder.FOLLOW, null, null);
            world.spawnEntity(s);
        }
    }

    private static void radio(ServerPlayerEntity player, RadioCommand c) {
        if (!player.isHolding(ModItems.RADIO)) return;
        ServerWorld world = player.getServerWorld();
        SoldierOrder order = SoldierOrder.values()[Math.max(0, Math.min(SoldierOrder.values().length - 1, c.order()))];
        var target = c.entityId() >= 0 ? world.getEntityById(c.entityId()) : null;
        int n = 0;
        for (SoldierEntity s : world.getEntitiesByClass(SoldierEntity.class, player.getBoundingBox().expand(256),
                s -> player.getUuid().equals(s.getLeader()))) {
            s.command(player.getUuid(), order, c.pos(), target != null ? target.getUuid() : null);
            n++;
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 0.8f, 1.6f);
        player.sendMessage(Text.translatable("radio.hiveinvasion.ack." + order.name().toLowerCase(), n).formatted(Formatting.GREEN), true);
    }
}
