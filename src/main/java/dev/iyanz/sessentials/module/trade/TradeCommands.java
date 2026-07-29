package dev.iyanz.sessentials.module.trade;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers the trade commands: {@code /trade <player>} to request a trade and
 * {@code /tradeaccept} (alias {@code /tradeyes}) to accept the pending request and open
 * the shared GUI. Both are gated on {@code sessentials.trade}.
 */
@SuppressWarnings("UnstableApiUsage")
final class TradeCommands {

    private TradeCommands() {
    }

    /**
     * Registers all trade commands.
     *
     * @param plugin  the owning plugin
     * @param manager the shared trade registry
     */
    static void register(SEssentialsPlugin plugin, TradeManager manager) {
        plugin.commands(reg -> {
            reg.register(buildTrade(plugin, manager), "Send a trade request to a player");
            reg.register(buildAccept(manager), "Accept the pending trade request", List.of("tradeyes"));
        });
    }

    private static LiteralCommandNode<CommandSourceStack> buildTrade(SEssentialsPlugin plugin, TradeManager manager) {
        return Commands.literal("trade")
                .requires(s -> s.getSender().hasPermission("sessentials.trade"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .executes(ctx -> {
                            Player requester = Cmds.player(ctx);
                            if (requester == null) {
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "player");
                            Player target = Bukkit.getPlayerExact(name);
                            if (target == null) {
                                Msg.err(requester, name + " is not online.");
                                return 0;
                            }
                            if (target.equals(requester)) {
                                Msg.err(requester, "You cannot trade with yourself.");
                                return 0;
                            }
                            if (manager.inSession(requester.getUniqueId())) {
                                Msg.err(requester, "You are already in a trade.");
                                return 0;
                            }
                            if (manager.inSession(target.getUniqueId())) {
                                Msg.err(requester, target.getName() + " is already in a trade.");
                                return 0;
                            }
                            manager.request(requester, target);
                            Msg.ok(requester, "Trade request sent to " + target.getName() + ".");
                            Schedulers.entity(plugin, target, () -> Msg.info(target, requester.getName()
                                    + " wants to trade. Use /tradeaccept (expires in 60s)."));
                            return 1;
                        }))
                .build();
    }

    private static LiteralCommandNode<CommandSourceStack> buildAccept(TradeManager manager) {
        return Commands.literal("tradeaccept")
                .requires(s -> s.getSender().hasPermission("sessentials.trade"))
                .executes(Cmds.playerExec(manager::accept))
                .build();
    }
}
