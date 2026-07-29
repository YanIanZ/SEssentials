package dev.iyanz.sessentials.module.chat;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /actionbar <player> <text...>}: sends a one-off action bar message
 * to a player. The text is operator-supplied and parsed as MiniMessage, sent on the
 * target's own region thread (Folia-safe).
 */
@SuppressWarnings("UnstableApiUsage")
public final class ActionBarCommand {

    private ActionBarCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    public static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("actionbar")
                    .requires(s -> s.getSender().hasPermission("sessentials.actionbar"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                    .executes(ctx -> send(plugin, ctx))))
                    .build();
            reg.register(node, "Send an action bar message to a player");
        });
    }

    private static int send(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        String text = StringArgumentType.getString(ctx, "text");
        Component message = Msg.mm(text);
        Schedulers.entity(plugin, target, () -> target.sendActionBar(message));
        Msg.ok(sender, "Sent an action bar message to " + target.getName() + ".");
        return 1;
    }
}
