package dev.iyanz.sessentials.module.itemcmds;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /clearinv <player>}: wipes another online player's entire inventory
 * (storage, armor and off-hand) on that player's Folia region thread. Console may
 * use it too. Self-clearing is intentionally not offered here — {@code /clear}
 * (aliases {@code /ci}, {@code /clearinventory}) already covers it.
 */
@SuppressWarnings("UnstableApiUsage")
final class ClearInvCommand {

    private ClearInvCommand() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("clearinv")
                        .requires(s -> s.getSender().hasPermission("sessentials.clearinv.others"))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(Cmds.PLAYERS)
                                .executes(ctx -> clear(plugin, ctx)))
                        .build(),
                "Clear another player's inventory"));
    }

    /**
     * Clears the named target's inventory on the target's region thread.
     *
     * @param plugin the owning plugin (for the region-thread hop)
     * @param ctx    the command context
     * @return 1 on success, 0 on error
     */
    private static int clear(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }

        Schedulers.entity(plugin, target, () -> {
            target.getInventory().clear();
            Msg.info(target, "Your inventory was cleared by " + sender.getName() + ".");
            Msg.ok(sender, "Cleared " + target.getName() + "'s inventory.");
        });
        return 1;
    }
}
