package dev.iyanz.sessentials.module.cmiextras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * {@code /oplist} — lists every server operator known to the ops file, both online and
 * offline, sorted with online operators first. Works for players and the console.
 * Requires {@code sessentials.oplist}.
 */
@SuppressWarnings("UnstableApiUsage")
final class OpListCommand {

    private OpListCommand() {
    }

    /**
     * Registers {@code /oplist}.
     *
     * @param reg the Paper command registrar
     */
    static void register(Commands reg) {
        reg.register(Commands.literal("oplist")
                .requires(s -> s.getSender().hasPermission("sessentials.oplist"))
                .executes(ctx -> list(ctx.getSource().getSender()))
                .build(), "List server operators (online and offline)");
    }

    /** Prints the sorted operator list to {@code sender}. */
    private static int list(CommandSender sender) {
        List<OfflinePlayer> ops = new ArrayList<>(Bukkit.getOperators());
        if (ops.isEmpty()) {
            Msg.info(sender, "No operators are set on this server.");
            return 1;
        }
        ops.sort(Comparator
                .comparing((OfflinePlayer op) -> !op.isOnline())
                .thenComparing(op -> nameOf(op).toLowerCase(Locale.ROOT)));

        Msg.info(sender, "Server operators (" + ops.size() + "):");
        for (OfflinePlayer op : ops) {
            String status = op.isOnline() ? Style.OK + "online" : Style.GRAY + "offline";
            Msg.raw(sender, Style.GRAY + "• " + Style.VALUE + nameOf(op) + " " + status);
        }
        return 1;
    }

    /**
     * @param op an operator entry
     * @return the operator's last-known name, or their UUID if no name is cached
     */
    private static String nameOf(OfflinePlayer op) {
        String name = op.getName();
        return name != null ? name : op.getUniqueId().toString();
    }
}
