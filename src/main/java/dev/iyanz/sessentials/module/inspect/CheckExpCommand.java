package dev.iyanz.sessentials.module.inspect;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /checkexp}: reports an online player's experience level and total
 * accumulated experience points ({@link Player#getLevel()}, {@link Player#getTotalExperience()}).
 */
@SuppressWarnings("UnstableApiUsage")
final class CheckExpCommand {

    private CheckExpCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("checkexp")
                    .requires(s -> s.getSender().hasPermission("sessentials.checkexp"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(CheckExpCommand::report))
                    .build();
            reg.register(node, "Show a player's level and total experience");
        });
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        Msg.value(sender, target.getName() + "'s level:", String.valueOf(target.getLevel()));
        Msg.value(sender, target.getName() + "'s total exp:", String.valueOf(target.getTotalExperience()));
        return 1;
    }
}
