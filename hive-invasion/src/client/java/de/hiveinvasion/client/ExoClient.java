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
 * Steuerung des Exo-Anzugs (Linksklick = Plasma, Rechtsklick = Flammenwerfer, G = Schild,
 * Leertaste = Jetpack, V = Scanner), Kamerafahrt beim Anlegen und Anzeige (HUD).
 */
public final class ExoClient {
    private static boolean lastFire, lastFlame, lastShield, lastBoost;
    public static final KeyBinding SHIELD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hiveinvasion.shield", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.hiveinvasion"));
    public static final KeyBinding SCANNER_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hiveinvasion.scanner", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.hiveinvasion"));
    private static Perspective savedPerspective;
    /** Spieler, die gerade einen Anzug anlegen (werden als Animationsfigur gezeichnet). */
    public static final java.util.Set<java.util.UUID> ASSEMBLING = new java.util.HashSet<>();
    private static ExoSuitEntity nearbySuit;

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
        ASSEMBLING.clear();
        if (mc.world != null) {
            for (var e : mc.world.getEntities()) {
                if (e instanceof ExoSuitEntity s && s.getAssembler().isPresent()) ASSEMBLING.add(s.getAssembler().get());
            }
        }
        // E am leeren Anzug: anlegen (sonst öffnet E wie gewohnt das Inventar)
        nearbySuit = null;
        if (mc.currentScreen == null && !mc.player.hasVehicle() && !ASSEMBLING.contains(mc.player.getUuid()) && mc.world != null) {
            nearbySuit = mc.world.getEntitiesByClass(ExoSuitEntity.class, mc.player.getBoundingBox().expand(2.2),
                    ExoSuitEntity::canBeEntered).stream()
                    .min(java.util.Comparator.comparingDouble(s -> s.squaredDistanceTo(mc.player))).orElse(null);
            if (nearbySuit != null) {
                boolean pressed = false;
                while (mc.options.inventoryKey.wasPressed()) pressed = true;
                if (pressed) ClientPlayNetworking.send(new de.hiveinvasion.exo.ExoEnterPayload(nearbySuit.getId()));
            }
        }
        boolean fire = false, flame = false, shield = false, boost = false;
        while (SCANNER_KEY.wasPressed()) Scanner.enabled = !Scanner.enabled;
        ExoSuitEntity suit = mc.player.getVehicle() instanceof ExoSuitEntity s ? s : null;

        if (suit != null && mc.currentScreen == null) {
            fire = held(mc, mc.options.attackKey);
            flame = held(mc, mc.options.useKey);
            shield = SHIELD_KEY.isPressed();
            boost = mc.options.jumpKey.isPressed();
            // Klicks verbrauchen, damit man im Anzug keine Blöcke abbaut oder setzt.
            while (mc.options.attackKey.wasPressed()) { }
            while (mc.options.useKey.wasPressed()) { }
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            suit.setInput(fire, flame, shield, boost);
        }
        if (fire != lastFire || flame != lastFlame || shield != lastShield || boost != lastBoost) {
            if (mc.getNetworkHandler() != null) ClientPlayNetworking.send(new ExoInputPayload(fire, flame, shield, boost));
            lastFire = fire; lastFlame = flame; lastShield = shield; lastBoost = boost;
        }

        // Kamerafahrt: während sich der Anzug schließt, Blick von vorn.
        boolean assembling = ASSEMBLING.contains(mc.player.getUuid());
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
        if (nearbySuit != null && mc.player != null && !mc.player.hasVehicle() && !mc.options.hudHidden) {
            Text hint = Text.translatable("hud.hiveinvasion.exo_enter", mc.options.inventoryKey.getBoundKeyLocalizedText());
            int tw = mc.textRenderer.getWidth(hint);
            ctx.drawTextWithShadow(mc.textRenderer, hint, (ctx.getScaledWindowWidth() - tw) / 2, ctx.getScaledWindowHeight() / 2 + 20, 0x80E0FF);
        }
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
        String status = suit.isShieldActive() ? "SCHILD AKTIV" : suit.isFlaming() ? "FLAMMENWERFER" : suit.isBoosting() ? "JETPACK" : "BEREIT";
        String scan = Scanner.enabled ? "SCANNER: " + Scanner.infectedCount() + " INFIZIERT" : "SCANNER AUS";
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(scan), x, y - 14, Scanner.infectedCount() > 0 ? 0xFF6060 : 0x80FF90);
        String keys = "LMB Plasma · RMB Flammen · " + SHIELD_KEY.getBoundKeyLocalizedText().getString() + " Schild · Leertaste Jetpack · "
                + SCANNER_KEY.getBoundKeyLocalizedText().getString() + " Scanner";
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(keys), w - mc.textRenderer.getWidth(keys) - 12, h - 18, 0x99CCDD);
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
