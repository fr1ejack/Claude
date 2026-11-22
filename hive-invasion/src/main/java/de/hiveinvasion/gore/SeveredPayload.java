package de.hiveinvasion.gore;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server → Client: welche Körperteile einem Lebewesen fehlen. */
public record SeveredPayload(int entityId, int mask) implements CustomPayload {
    public static final Id<SeveredPayload> ID = new Id<>(HiveInvasion.id("severed"));
    public static final PacketCodec<RegistryByteBuf, SeveredPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SeveredPayload::entityId, PacketCodecs.VAR_INT, SeveredPayload::mask, SeveredPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
