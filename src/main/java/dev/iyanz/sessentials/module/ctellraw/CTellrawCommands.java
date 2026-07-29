package dev.iyanz.sessentials.module.ctellraw;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers and handles the operator raw-MiniMessage commands: {@code /ctellraw}
 * (send a rich message to one online player) and {@code /ctextall} (broadcast a
 * rich message to every online player and the console).
 *
 * <p>Unlike the plain-English {@link Msg} helpers used elsewhere, the message text
 * here is parsed as MiniMessage as-is — gradients, {@code <click:run_command:...>},
 * {@code <hover:show_text:...>}, etc. all work. This is intentional: both commands
 * are gated behind the {@code sessentials.ctellraw} operator permission, so the
 * input is trusted staff-authored rich text, not untrusted player chat.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class CTellrawCommands {

    /** Shared permission for both commands (see class Javadoc). */
    private static final String PERMISSION = "sessentials.ctellraw";

    private CTellrawCommands() {
    }

    /** Registers {@code /ctellraw} and {@code /ctextall} (alias {@code /ctellrawall}). */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("ctellraw")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                    .executes(ctx -> handleTellraw(plugin, ctx))))
                    .build(), "Send a raw MiniMessage message to a player", List.of());

            reg.register(Commands.literal("ctextall")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ctx -> handleTellrawAll(plugin, ctx)))
                    .build(), "Broadcast a raw MiniMessage message to everyone", List.of("ctellrawall"));
        });
    }

    /**
     * Parses the message as MiniMessage and delivers it to the named online player,
     * on that player's own region thread (Folia-safe). Errors to the sender if the
     * target is not online.
     */
    private static int handleTellraw(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "target");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(ctx.getSource().getSender(), name + " is not online.");
            return 0;
        }
        Component message = Msg.mm(StringArgumentType.getString(ctx, "message"));
        Schedulers.entity(plugin, target, () -> target.sendMessage(message));
        Msg.ok(ctx.getSource().getSender(), "Message sent to " + target.getName() + ".");
        return 1;
    }

    /**
     * Parses the message as MiniMessage and delivers it to the console and every
     * online player, each on its own region thread (Folia-safe).
     */
    private static int handleTellrawAll(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Component message = Msg.mm(StringArgumentType.getString(ctx, "message"));
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> player.sendMessage(message));
        }
        Msg.ok(ctx.getSource().getSender(), "Message broadcast to everyone.");
        return 1;
    }
}
