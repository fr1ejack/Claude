package de.hiveinvasion.exo;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → Server: Spieler hat am Anzug E gedrückt. */
public record ExoEnterPayload(int suitId) implements CustomPayload {
    public static final Id<ExoEnterPayload> ID = new Id<>(HiveInvasion.id("exo_enter"));
    public static final PacketCodec<RegistryByteBuf, ExoEnterPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, ExoEnterPayload::suitId, ExoEnterPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
