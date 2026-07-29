package dev.iyanz.sessentials.module.playtime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /firstjoined} (alias {@code /firstseen}): reports when a player
 * first connected to the server, using {@link OfflinePlayer#getFirstPlayed()}. Unlike
 * {@code /playtime}, this works for offline players too, since the first-played
 * timestamp is stored regardless of whether the target is currently online.
 */
@SuppressWarnings("UnstableApiUsage")
final class FirstJoinedCommand {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private FirstJoinedCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("firstjoined")
                    .requires(s -> s.getSender().hasPermission("sessentials.firstjoined"))
                    .executes(Cmds.playerExec(FirstJoinedCommand::reportSelf))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .requires(s -> s.getSender().hasPermission("sessentials.firstjoined.others"))
                            .executes(FirstJoinedCommand::reportOther))
                    .build();
            reg.register(node, "Show when a player first joined", List.of("firstseen"));
        });
    }

    private static void reportSelf(Player player) {
        report(player, player.getName(), player.getFirstPlayed());
    }

    @SuppressWarnings("deprecation")
    private static int reportOther(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        String displayName = target.getName() != null ? target.getName() : name;
        return report(sender, displayName, target.getFirstPlayed());
    }

    private static int report(CommandSender sender, String name, long firstPlayedMillis) {
        if (firstPlayedMillis <= 0L) {
            Msg.err(sender, "No record of " + name + ".");
            return 0;
        }
        String date = DATE_FORMAT.format(Instant.ofEpochMilli(firstPlayedMillis));
        Msg.value(sender, name + " first joined", date + " (" + daysAgo(firstPlayedMillis) + ")");
        return 1;
    }

    private static String daysAgo(long firstPlayedMillis) {
        long days = Math.max(0L, (System.currentTimeMillis() - firstPlayedMillis) / 86_400_000L);
        if (days == 0L) {
            return "today";
        }
        return days == 1L ? "1 day ago" : days + " days ago";
    }
}
