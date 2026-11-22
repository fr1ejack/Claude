package de.hiveinvasion.client.screen;

import de.hiveinvasion.military.MilitaryNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Gespräch mit der Torwache: beitreten, angreifen oder gehen. */
public class GuardDialogScreen extends Screen {
    private final int soldierId;
    private final boolean member;

    public GuardDialogScreen(int soldierId, boolean member) {
        super(Text.translatable("dialog.hiveinvasion.title"));
        this.soldierId = soldierId;
        this.member = member;
    }

    @Override
    protected void init() {
        int w = 260, x = (width - w) / 2, y = height / 2 - 10;
        if (member) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("dialog.hiveinvasion.reinforce"), b -> send(2)).dimensions(x, y, w, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.translatable("dialog.hiveinvasion.join"), b -> send(0)).dimensions(x, y, w, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("dialog.hiveinvasion.attack"), b -> send(1)).dimensions(x, y + 24, w, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("dialog.hiveinvasion.leave"), b -> close()).dimensions(x, y + 48, w, 20).build());
    }

    private void send(int choice) {
        ClientPlayNetworking.send(new MilitaryNet.DialogChoice(soldierId, choice));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2, y = height / 2 - 60;
        ctx.fill(cx - 150, y - 10, cx + 150, y + 36, 0xB0101418);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("dialog.hiveinvasion.guard_name"), cx, y, 0xC8D890);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable(member ? "dialog.hiveinvasion.greet_member" : "dialog.hiveinvasion.greet"), cx, y + 16, 0xFFFFFF);
    }

    @Override public boolean shouldPause() { return false; }
}
