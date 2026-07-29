package dev.iyanz.sessentials.module.identity;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /realname}: looks up the online player whose current nickname or
 * real name matches the given text (case-insensitive) and reports their real
 * (account) name.
 */
@SuppressWarnings("UnstableApiUsage")
public final class RealnameCommand {

    private RealnameCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    public static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("realname")
                    .requires(s -> s.getSender().hasPermission("sessentials.realname"))
                    .then(Commands.argument("nick", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String query = StringArgumentType.getString(ctx, "nick");
                                Player match = find(query);
                                if (match == null) {
                                    Msg.err(sender, "No player found matching '" + query + "'.");
                                    return 0;
                                }
                                Msg.value(sender, query + "'s real name is", match.getName());
                                return 1;
                            }))
                    .build();
            reg.register(node, "Look up a player's real name from their nickname");
        });
    }

    /** @return the online player whose display name (nick) or real name matches {@code query}, or {@code null}. */
    private static Player find(String query) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(query)) {
                return online;
            }
            String plainDisplay = PlainTextComponentSerializer.plainText().serialize(online.displayName());
            if (plainDisplay.equalsIgnoreCase(query)) {
                return online;
            }
        }
        return null;
    }
}
