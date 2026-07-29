package dev.iyanz.sessentials.module.inspect;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Registers {@code /banlist}: lists every currently name-banned player with their
 * reason and expiry (or {@code "permanent"} if none), read from the server's
 * {@link BanList#getBanEntries() name ban list}.
 *
 * <p>Uses the classic {@code BanList.Type.NAME}-based API (also used by the
 * moderation module's {@code /ban}/{@code /unban}), which Paper has since
 * deprecated in favour of a generic {@code BanListType}-based one; the classic API
 * still works and keeps this command consistent with the rest of the plugin.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation", "rawtypes"})
final class BanListCommand {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private BanListCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("banlist")
                    .requires(s -> s.getSender().hasPermission("sessentials.banlist"))
                    .executes(ctx -> report(ctx.getSource().getSender()))
                    .build();
            reg.register(node, "List currently banned players");
        });
    }

    private static int report(CommandSender sender) {
        BanList<?> banList = Bukkit.getBanList(BanList.Type.NAME);
        Set<BanEntry> entries = banList.getBanEntries();
        if (entries.isEmpty()) {
            Msg.info(sender, "No players are currently banned.");
            return 1;
        }
        Msg.info(sender, entries.size() + " banned player(s):");
        for (BanEntry entry : entries) {
            String reason = entry.getReason() != null ? entry.getReason() : "no reason given";
            String expiry = entry.getExpiration() != null
                    ? "expires " + EXPIRY_FORMAT.format(entry.getExpiration().toInstant())
                    : "permanent";
            Msg.value(sender, entry.getTarget() + ":", reason + " (" + expiry + ")");
        }
        return 1;
    }
}
