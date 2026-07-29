package dev.iyanz.sessentials.module.inspect;

import java.net.InetSocketAddress;
import java.util.List;

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
 * Registers {@code /ip} (alias {@code /getip}): shows an online player's connecting
 * IP address, via {@link Player#getAddress()} — a simple, already-resolved read.
 */
@SuppressWarnings("UnstableApiUsage")
final class IpCommand {

    private IpCommand() {
    }

    /**
     * Registers the command against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            var node = Commands.literal("ip")
                    .requires(s -> s.getSender().hasPermission("sessentials.ip"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(IpCommand::report))
                    .build();
            reg.register(node, "Show an online player's IP address", List.of("getip"));
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
        InetSocketAddress address = target.getAddress();
        if (address == null || address.getAddress() == null) {
            Msg.err(sender, "Could not resolve " + target.getName() + "'s address.");
            return 0;
        }
        Msg.value(sender, target.getName() + "'s IP:", address.getAddress().getHostAddress());
        return 1;
    }
}
