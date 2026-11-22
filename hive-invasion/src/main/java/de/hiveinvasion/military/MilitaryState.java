package de.hiveinvasion.military;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Wer ist beim Militär, und welche Stützpunkte sind wem feindlich gesinnt? */
public class MilitaryState extends PersistentState {
    private final Set<UUID> members = new HashSet<>();
    /** Stützpunkt (gerundete Position) → feindliche Spieler */
    private final Map<Long, Set<UUID>> hostile = new HashMap<>();

    public static MilitaryState get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(MilitaryState::new, MilitaryState::read, null), "hiveinvasion_military");
    }

    private static long key(BlockPos base) { return BlockPos.asLong(base.getX() >> 7, 0, base.getZ() >> 7); }

    public boolean isMember(UUID player) { return members.contains(player); }
    public void join(UUID player) { members.add(player); markDirty(); }

    public boolean isHostile(UUID player, BlockPos base) {
        Set<UUID> s = hostile.get(key(base));
        return s != null && s.contains(player);
    }

    public void setHostile(UUID player, BlockPos base) {
        hostile.computeIfAbsent(key(base), k -> new HashSet<>()).add(player);
        members.remove(player);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList m = new NbtList();
        for (UUID u : members) m.add(NbtString.of(u.toString()));
        nbt.put("Members", m);
        NbtCompound h = new NbtCompound();
        hostile.forEach((k, set) -> {
            NbtList l = new NbtList();
            for (UUID u : set) l.add(NbtString.of(u.toString()));
            h.put(Long.toString(k), l);
        });
        nbt.put("Hostile", h);
        return nbt;
    }

    private static MilitaryState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        MilitaryState s = new MilitaryState();
        for (NbtElement e : nbt.getList("Members", NbtElement.STRING_TYPE)) s.members.add(UUID.fromString(e.asString()));
        NbtCompound h = nbt.getCompound("Hostile");
        for (String k : h.getKeys()) {
            Set<UUID> set = new HashSet<>();
            for (NbtElement e : h.getList(k, NbtElement.STRING_TYPE)) set.add(UUID.fromString(e.asString()));
            s.hostile.put(Long.parseLong(k), set);
        }
        return s;
    }
}
