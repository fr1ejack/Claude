package de.hiveinvasion.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Gespeicherter Zustand aller Städte (Lage, Name, Baufortschritt, Nest, Angriffe). */
public class WorldState extends PersistentState {
    public static final class City {
        public final int rx, rz, x0, z0;
        public int y, stage, hiveX, hiveZ, hiveY;
        public String name = "Stadt";
        public long nextAttack;
        public boolean hiveBuilt;

        public City(int rx, int rz, int x0, int z0) { this.rx = rx; this.rz = rz; this.x0 = x0; this.z0 = z0; }
        public int centerX() { return x0 + CityBuilder.SIZE / 2; }
        public int centerZ() { return z0 + CityBuilder.SIZE / 2; }
        public int baseX() { return x0 + CityBuilder.SIZE + 10; }
        public int baseZ() { return centerZ() - BaseBuilder.D / 2; }

        NbtCompound write() {
            NbtCompound n = new NbtCompound();
            n.putInt("rx", rx); n.putInt("rz", rz); n.putInt("x0", x0); n.putInt("z0", z0); n.putInt("y", y);
            n.putInt("stage", stage == 1 ? 0 : stage);   // halb gebaute Städte nach Neustart neu bauen
            n.putInt("hx", hiveX); n.putInt("hz", hiveZ); n.putInt("hy", hiveY); n.putString("name", name);
            n.putLong("attack", nextAttack); n.putBoolean("hive", hiveBuilt);
            return n;
        }

        static City read(NbtCompound n) {
            City c = new City(n.getInt("rx"), n.getInt("rz"), n.getInt("x0"), n.getInt("z0"));
            c.y = n.getInt("y"); c.stage = n.getInt("stage"); c.hiveX = n.getInt("hx"); c.hiveZ = n.getInt("hz"); c.hiveY = n.getInt("hy");
            c.name = n.getString("name"); c.nextAttack = n.getLong("attack"); c.hiveBuilt = n.getBoolean("hive");
            return c;
        }
    }

    private final Map<Long, City> cities = new HashMap<>();

    public static WorldState get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(WorldState::new, WorldState::read, null), "hiveinvasion_cities");
    }

    private static long key(int rx, int rz) { return ((long) rx << 32) | (rz & 0xffffffffL); }

    public City city(int rx, int rz) { return cities.get(key(rx, rz)); }
    public void put(City c) { cities.put(key(c.rx, c.rz), c); markDirty(); }
    public Collection<City> all() { return cities.values(); }

    /** Nächste fertige Stadt zu einer Position. */
    public City nearest(double x, double z) {
        City best = null; double bd = Double.MAX_VALUE;
        for (City c : cities.values()) {
            if (c.stage != 2) continue;
            double d = (c.centerX() - x) * (c.centerX() - x) + (c.centerZ() - z) * (c.centerZ() - z);
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList l = new NbtList();
        for (City c : cities.values()) l.add(c.write());
        nbt.put("Cities", l);
        return nbt;
    }

    private static WorldState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        WorldState s = new WorldState();
        for (NbtElement e : nbt.getList("Cities", NbtElement.COMPOUND_TYPE)) {
            City c = City.read((NbtCompound) e);
            s.cities.put(key(c.rx, c.rz), c);
        }
        return s;
    }
}
