package de.hiveinvasion.world;

import net.minecraft.util.math.random.Random;

/** Zufällige Städtenamen. */
public final class Names {
    private static final String[] A = {"Neu", "Ober", "Unter", "Bad ", "Groß", "Klein", "Alt", "Nord", "Süd", "West", "Ost", ""};
    private static final String[] B = {"Hagen", "Brück", "Ried", "Linden", "Eichen", "Stein", "Marien", "Wald", "Rosen", "Falken", "Kron", "Silber", "Eisen", "Hafen"};
    private static final String[] C = {"burg", "stadt", "dorf", "feld", "heim", "hausen", "berg", "tal", "furt", "au"};

    private Names() {}

    public static String city(Random r) {
        String a = A[r.nextInt(A.length)], b = B[r.nextInt(B.length)], c = C[r.nextInt(C.length)];
        String s = a + (a.isEmpty() || a.endsWith(" ") ? b : b.toLowerCase()) + c;
        return s.trim();
    }
}
