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
 * Registers {@code /checkperm}: reports whether an online player currently holds a
 * given permission node, via {@link Player#hasPermission(String)} — a simple,
 * already-resolved read, safe without hopping to the target's region thread.
 */
@SuppressWarnings("UnstableApiUsage")
final class CheckPermCommand {

    private CheckPermCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("checkperm")
                    .requires(s -> s.getSender().hasPermission("sessentials.checkperm"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("permission", StringArgumentType.word())
                                    .executes(CheckPermCommand::report)))
                    .build();
            reg.register(node, "Check whether an online player has a permission");
        });
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        String permission = StringArgumentType.getString(ctx, "permission");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        boolean has = target.hasPermission(permission);
        Msg.value(sender, target.getName() + " has '" + permission + "':", has ? "yes" : "no");
        return 1;
    }
}
