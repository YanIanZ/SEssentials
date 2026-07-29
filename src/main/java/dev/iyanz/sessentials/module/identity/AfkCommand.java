package dev.iyanz.sessentials.module.identity;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers {@code /afk}: toggles the sender's AFK state and broadcasts the change
 * to every online player, with an optional reason message.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AfkCommand {

    private AfkCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin     the owning plugin
     * @param afkService the shared AFK-state tracker
     */
    public static void register(SEssentialsPlugin plugin, AfkService afkService) {
        plugin.commands(reg -> {
            var node = Commands.literal("afk")
                    .requires(s -> s.getSender().hasPermission("sessentials.afk"))
                    .executes(Cmds.playerExec(p -> toggle(afkService, p, null)))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                Player p = Cmds.player(ctx);
                                if (p == null) {
                                    return 0;
                                }
                                toggle(afkService, p, StringArgumentType.getString(ctx, "message"));
                                return 1;
                            }))
                    .build();
            reg.register(node, "Toggle your AFK status");
        });
    }

    private static void toggle(AfkService afkService, Player player, String message) {
        boolean nowAfk = afkService.toggle(player.getUniqueId());
        String line = nowAfk
                ? player.getName() + " is now AFK" + (message != null && !message.isBlank() ? ": " + message : "")
                : player.getName() + " is no longer AFK";
        for (Player online : Bukkit.getOnlinePlayers()) {
            Msg.info(online, line);
        }
    }
}
