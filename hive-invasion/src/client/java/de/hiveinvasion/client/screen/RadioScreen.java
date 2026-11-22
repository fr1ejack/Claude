package de.hiveinvasion.client.screen;

import de.hiveinvasion.human.SoldierOrder;
import de.hiveinvasion.military.MilitaryNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Funkgerät: Befehl für den Trupp auswählen (Ziel = worauf man geschaut hat). */
public class RadioScreen extends Screen {
    private final BlockPos pos;
    private final int entityId;
    private final Text targetName;

    public RadioScreen(BlockPos pos, int entityId, Text targetName) {
        super(Text.translatable("radio.hiveinvasion.title"));
        this.pos = pos; this.entityId = entityId; this.targetName = targetName;
    }

    @Override
    protected void init() {
        int w = 200, cx = width / 2, cy = height / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("radio.hiveinvasion.follow"), b -> send(SoldierOrder.FOLLOW)).dimensions(cx - w / 2, cy - 30, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("radio.hiveinvasion.move"), b -> send(SoldierOrder.MOVE)).dimensions(cx - w / 2, cy - 6, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("radio.hiveinvasion.return"), b -> send(SoldierOrder.RETURN)).dimensions(cx - w / 2, cy + 18, w, 20).build());
        ButtonWidget attack = ButtonWidget.builder(Text.translatable("radio.hiveinvasion.attack"), b -> send(SoldierOrder.ATTACK)).dimensions(cx - w / 2, cy + 42, w, 20).build();
        addDrawableChild(attack);
    }

    private void send(SoldierOrder order) {
        ClientPlayNetworking.send(new MilitaryNet.RadioCommand(order.ordinal(), pos, entityId));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2, y = height / 2 - 70;
        ctx.fill(cx - 120, y - 8, cx + 120, y + 30, 0xC0081008);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("radio.hiveinvasion.title"), cx, y, 0x60FF60);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("radio.hiveinvasion.target", targetName), cx, y + 14, 0xC0FFC0);
    }

    @Override public boolean shouldPause() { return false; }
}
