package dev.iyanz.sessentials.module.identity;

import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /seen} (alias {@code /lastseen}): reports whether a player is
 * online right now, or how long ago they last disconnected, using the
 * {@code "identity"} data store maintained by {@link IdentityListener}.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SeenCommand {

    private static final String KEY_LAST = ".last";
    private static final String KEY_NAME = ".name";

    private SeenCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    public static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("seen")
                    .requires(s -> s.getSender().hasPermission("sessentials.seen"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .executes(ctx -> report(plugin, ctx)))
                    .build();
            reg.register(node, "Show when a player was last online", List.of("lastseen"));
        });
    }

    @SuppressWarnings("deprecation")
    private static int report(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            Msg.value(sender, online.getName(), "online now");
            return 1;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        UUID uuid = offline.getUniqueId();
        YamlStore store = plugin.stores().get("identity");
        String lastKey = uuid + KEY_LAST;
        if (!store.contains(lastKey)) {
            Msg.err(sender, "No record of " + name + ".");
            return 0;
        }

        long lastMillis = Long.parseLong(store.getString(lastKey));
        String knownName = store.getString(uuid + KEY_NAME);
        String displayName = knownName != null ? knownName : name;
        Msg.value(sender, displayName + " was last seen",
                formatElapsed(System.currentTimeMillis() - lastMillis) + " ago");
        return 1;
    }

    /** @return a short {@code "2d 3h"}-style rendering of an elapsed duration. */
    private static String formatElapsed(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
