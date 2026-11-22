package de.hiveinvasion.net;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Eingaben des Piloten im Exo-Anzug: Schießen, Schild, Sprungdüsen. */
public record ExoInputPayload(boolean fire, boolean shield, boolean boost) implements CustomPayload {
    public static final Id<ExoInputPayload> ID = new Id<>(HiveInvasion.id("exo_input"));
    public static final PacketCodec<RegistryByteBuf, ExoInputPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, ExoInputPayload::fire,
            PacketCodecs.BOOL, ExoInputPayload::shield,
            PacketCodecs.BOOL, ExoInputPayload::boost,
            ExoInputPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
