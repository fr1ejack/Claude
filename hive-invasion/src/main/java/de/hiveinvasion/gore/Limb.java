package de.hiveinvasion.gore;

/** Abtrennbare Körperteile. Werden als Bitmaske gespeichert. */
public enum Limb {
    HEAD, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

    public int bit() { return 1 << ordinal(); }

    public boolean in(int mask) { return (mask & bit()) != 0; }

    public static Limb byIndex(int i) { return values()[Math.floorMod(i, values().length)]; }
}
