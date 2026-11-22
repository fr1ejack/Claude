package de.hiveinvasion.client;

import de.hiveinvasion.gore.Limb;
import de.hiveinvasion.gore.SeveredPayload;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;

/** Merkt sich auf dem Client, welche Körperteile welchem Lebewesen fehlen. */
public final class ClientGore {
    private static final Int2IntOpenHashMap MASKS = new Int2IntOpenHashMap();

    private ClientGore() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SeveredPayload.ID, (payload, ctx) -> MASKS.put(payload.entityId(), payload.mask()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MASKS.clear());
    }

    public static int mask(Entity e) {
        return MASKS.get(e.getId());
    }

    /** Blendet abgetrennte Teile eines Menschenmodells aus; gibt die alten Sichtbarkeiten zurück. */
    public static boolean[] hide(BipedEntityModel<?> m, int mask) {
        ModelPart[] parts = {m.head, m.hat, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
        boolean[] old = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) old[i] = parts[i].visible;
        if (Limb.HEAD.in(mask)) { m.head.visible = false; m.hat.visible = false; }
        if (Limb.RIGHT_ARM.in(mask)) m.rightArm.visible = false;
        if (Limb.LEFT_ARM.in(mask)) m.leftArm.visible = false;
        if (Limb.RIGHT_LEG.in(mask)) m.rightLeg.visible = false;
        if (Limb.LEFT_LEG.in(mask)) m.leftLeg.visible = false;
        return old;
    }

    public static void restore(BipedEntityModel<?> m, boolean[] old) {
        ModelPart[] parts = {m.head, m.hat, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
        for (int i = 0; i < parts.length; i++) parts[i].visible = old[i];
    }
}
