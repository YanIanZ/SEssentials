package dev.iyanz.sessentials.module.messaging;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers and handles the server-wide messaging commands: the operator
 * {@code /broadcast} announcement and the player {@code /me} action line.
 *
 * <p>{@code /broadcast} is operator-facing (gated by {@code sessentials.broadcast})
 * and its text is parsed as MiniMessage so operators can format announcements.
 * {@code /me}'s action text is player-supplied and is always sent as plain text —
 * never parsed as MiniMessage — so a player cannot inject markup into a server-wide
 * message.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class BroadcastCommands {

    private BroadcastCommands() {
    }

    /** Registers {@code /broadcast} and {@code /me}. */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("broadcast")
                    .requires(s -> s.getSender().hasPermission("sessentials.broadcast"))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ctx -> handleBroadcast(plugin, ctx)))
                    .build(), "Server-wide announcement", List.of("bc"));

            reg.register(Commands.literal("me")
                    .requires(s -> s.getSender().hasPermission("sessentials.me"))
                    .then(Commands.argument("action", StringArgumentType.greedyString())
                            .executes(ctx -> handleMe(plugin, ctx)))
                    .build(), "Broadcast an action", List.of());
        });
    }

    private static int handleBroadcast(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "message");
        Component prefix = Msg.mm(Style.GRAY + "[" + Style.INFO + SmallCaps.of("Broadcast") + Style.GRAY + "] ");
        Component full = prefix.append(Msg.mm(text));
        broadcast(plugin, full);
        return 1;
    }

    private static int handleMe(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        String action = StringArgumentType.getString(ctx, "action");
        Component prefix = Msg.mm(Style.GRAY + "* " + Style.VALUE + sender.getName() + " ");
        Component full = prefix.append(Component.text(action, NamedTextColor.WHITE));
        broadcast(plugin, full);
        return 1;
    }

    /** Sends {@code message} to the console and to every online player, each on its own region thread. */
    private static void broadcast(SEssentialsPlugin plugin, Component message) {
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
    }
}
