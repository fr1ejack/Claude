package de.hiveinvasion.client;

import de.hiveinvasion.exo.ExoSuitEntity;
import de.hiveinvasion.net.ExoInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Steuerung des Exo-Anzugs (Linksklick = Feuer, Rechtsklick halten = Schild,
 * Leertaste halten = Sprungdüsen), Kamerafahrt beim Anlegen und Anzeige (HUD).
 */
public final class ExoClient {
    private static boolean lastFire, lastShield, lastBoost;
    private static Perspective savedPerspective;

    private ExoClient() {}

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(ExoClient::tick);
        HudRenderCallback.EVENT.register((ctx, counter) -> hud(ctx));
    }

    private static boolean held(MinecraftClient mc, KeyBinding key) {
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(key);
        long win = mc.getWindow().getHandle();
        if (bound.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(win, bound.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(win, bound.getCode());
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        boolean fire = false, shield = false, boost = false;
        ExoSuitEntity suit = mc.player.getVehicle() instanceof ExoSuitEntity s ? s : null;

        if (suit != null && mc.currentScreen == null) {
            fire = held(mc, mc.options.attackKey);
            shield = held(mc, mc.options.useKey);
            boost = mc.options.jumpKey.isPressed();
            // Klicks verbrauchen, damit man im Anzug keine Blöcke abbaut oder setzt.
            while (mc.options.attackKey.wasPressed()) { }
            while (mc.options.useKey.wasPressed()) { }
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            suit.setInput(fire, shield, boost);
        }
        if (fire != lastFire || shield != lastShield || boost != lastBoost) {
            if (mc.getNetworkHandler() != null) ClientPlayNetworking.send(new ExoInputPayload(fire, shield, boost));
            lastFire = fire; lastShield = shield; lastBoost = boost;
        }

        // Kamerafahrt: während sich der Anzug schließt, Blick von vorn.
        boolean assembling = mc.world != null && mc.world.getEntitiesByClass(ExoSuitEntity.class,
                mc.player.getBoundingBox().expand(2), e -> e.getAssembler().map(mc.player.getUuid()::equals).orElse(false)).size() > 0;
        if (assembling && savedPerspective == null) {
            savedPerspective = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        } else if (!assembling && savedPerspective != null) {
            mc.options.setPerspective(savedPerspective);
            savedPerspective = null;
        }
    }

    private static void hud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof ExoSuitEntity suit) || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

        // Visier-Rahmen
        int edge = 0x3300C8FF;
        ctx.fill(0, 0, w, 6, edge);
        ctx.fill(0, h - 6, w, h, edge);
        ctx.fill(0, 0, 6, h, edge);
        ctx.fill(w - 6, 0, w, h, edge);

        int x = 12, y = h - 64;
        bar(ctx, x, y, 110, suit.getHealth() / suit.getMaxHealth(), 0xFFE04040, "PANZERUNG");
        bar(ctx, x, y + 18, 110, suit.getEnergy() / ExoSuitEntity.MAX_ENERGY, 0xFF40C8FF, "ENERGIE");
        String status = suit.isShieldActive() ? "SCHILD AKTIV" : suit.isBoosting() ? "DÜSEN" : "BEREIT";
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(status), x, y + 36, suit.isShieldActive() ? 0x80E0FF : 0xA0FFA0);
        // Fadenkreuz-Ring
        int cx = w / 2, cy = h / 2;
        ctx.fill(cx - 9, cy - 1, cx - 5, cy + 1, 0xAA60D0FF);
        ctx.fill(cx + 5, cy - 1, cx + 9, cy + 1, 0xAA60D0FF);
        ctx.fill(cx - 1, cy - 9, cx + 1, cy - 5, 0xAA60D0FF);
        ctx.fill(cx - 1, cy + 5, cx + 1, cy + 9, 0xAA60D0FF);
    }

    private static void bar(DrawContext ctx, int x, int y, int width, float frac, int color, String label) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x, y - 1, 0xDDEEFF);
        int bx = x, by = y + 9;
        ctx.fill(bx - 1, by - 1, bx + width + 1, by + 5, 0xAA000000);
        ctx.fill(bx, by, bx + (int) (width * Math.max(0, Math.min(1, frac))), by + 4, color);
    }
}
