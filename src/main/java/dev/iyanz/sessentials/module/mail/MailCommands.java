package dev.iyanz.sessentials.module.mail;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers and handles {@code /mail send|read|clear}: leaves offline-friendly mail
 * for a player, lists your own mail newest-first, and clears it. See
 * {@link MailService} for the underlying storage format.
 *
 * <p>Mail text is player-supplied and is never parsed as MiniMessage — it is
 * appended as a plain {@link Component#text(String)} so a message cannot inject
 * colour tags or click events into another player's chat.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class MailCommands {

    private MailCommands() {
    }

    /** Registers the {@code /mail} command tree, gated by {@code sessentials.mail}. */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(Commands.literal("mail")
                .requires(s -> s.getSender().hasPermission("sessentials.mail"))
                .then(Commands.literal("send")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> handleSend(plugin, ctx)))))
                .then(Commands.literal("read")
                        .executes(Cmds.playerExec(p -> handleRead(plugin, p))))
                .then(Commands.literal("clear")
                        .executes(Cmds.playerExec(p -> handleClear(plugin, p))))
                .build(), "Leave, read or clear player mail"));
    }

    @SuppressWarnings("deprecation")
    private static int handleSend(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "target");
        String message = StringArgumentType.getString(ctx, "message");

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            Msg.err(sender, name + " has never played on this server.");
            return 0;
        }

        MailService.add(plugin, target.getUniqueId(), sender.getName(), message);
        Msg.ok(sender, "Mail sent to " + name + ".");
        return 1;
    }

    private static void handleRead(SEssentialsPlugin plugin, Player player) {
        YamlStore store = MailService.store(plugin);
        List<MailService.Entry> entries = MailService.decode(MailService.rawMessages(store, player.getUniqueId()));
        if (entries.isEmpty()) {
            Msg.info(player, "You have no mail.");
            return;
        }

        Msg.info(player, "Your mail (" + entries.size() + "):");
        for (int i = entries.size() - 1; i >= 0; i--) {
            player.sendMessage(formatLine(entries.get(i)));
        }
        MailService.markRead(plugin, player.getUniqueId());
    }

    private static void handleClear(SEssentialsPlugin plugin, Player player) {
        YamlStore store = MailService.store(plugin);
        int count = MailService.rawMessages(store, player.getUniqueId()).size();
        MailService.clear(plugin, player.getUniqueId());
        if (count == 0) {
            Msg.info(player, "You have no mail to clear.");
        } else {
            Msg.ok(player, "Cleared " + count + " mail message" + (count == 1 ? "" : "s") + ".");
        }
    }

    /** {@code [from • 3h ago] text} — sender name is small-capped, text is a plain component. */
    private static Component formatLine(MailService.Entry entry) {
        String ago = relative(System.currentTimeMillis() - entry.epochMillis());
        Component prefix = Msg.mm(Style.GRAY + "[" + Style.INFO + SmallCaps.of(entry.fromName())
                + Style.GRAY + " • " + ago + " ago] ");
        return prefix.append(Component.text(entry.text(), NamedTextColor.WHITE));
    }

    /** @return a short {@code "2d 3h"}-style rendering of an elapsed duration. */
    private static String relative(long millis) {
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
