package de.hiveinvasion.world;

import de.hiveinvasion.hive.HiveBuilder;
import de.hiveinvasion.hive.HiveDirector;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** /hiveinvasion stadt  – nächste Stadt und Nest anzeigen;  /hiveinvasion angriff – Angriff jetzt auslösen. */
public final class HiveCommands {
    private HiveCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                CommandManager.literal("hiveinvasion").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("stadt").executes(ctx -> info(ctx.getSource())))
                        .then(CommandManager.literal("angriff").executes(ctx -> attack(ctx.getSource())))));
    }

    private static WorldState.City nearest(ServerCommandSource src) {
        return WorldState.get(src.getWorld()).nearest(src.getPosition().x, src.getPosition().z);
    }

    private static int info(ServerCommandSource src) {
        WorldState.City c = nearest(src);
        if (c == null) { src.sendError(Text.literal("Noch keine fertige Stadt.")); return 0; }
        HiveBuilder.Layout l = HiveBuilder.layout(c, src.getWorld().getSeed());
        src.sendFeedback(() -> Text.literal(c.name + ": Zentrum " + c.centerX() + " " + c.y + " " + c.centerZ()
                + " | Stützpunkt " + c.baseX() + " " + c.baseZ() + " | Königin " + l.queen.toShortString()
                + " | Eier " + l.eggs.toShortString() + " | Kokons " + l.cocoons.toShortString() + " | Ausgang " + l.exit1.getX() + " " + l.exit1.getZ()), false);
        return 1;
    }

    private static int attack(ServerCommandSource src) {
        WorldState.City c = nearest(src);
        if (c == null || !c.hiveBuilt) { src.sendError(Text.literal("Keine Stadt mit Nest in der Nähe.")); return 0; }
        HiveDirector.attack(src.getWorld(), c, HiveBuilder.layout(c, src.getWorld().getSeed()));
        return 1;
    }
}
