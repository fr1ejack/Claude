package de.hiveinvasion.net;

import de.hiveinvasion.HiveInvasion;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/** Server → Pilot: Visier-Scan. Welche Lebewesen in der Nähe infiziert sind. */
public record ScanPayload(List<Integer> infected) implements CustomPayload {
    public static final Id<ScanPayload> ID = new Id<>(HiveInvasion.id("scan"));
    public static final PacketCodec<RegistryByteBuf, ScanPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), ScanPayload::infected, ScanPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
