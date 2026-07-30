package dev.iyanz.sessentials.module.cmiextras;

import java.net.InetSocketAddress;

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
 * {@code /tempipban <player> <duration>} — temporarily IP-bans an online player: their
 * address is stored with an expiry (epoch millis) in the {@code tempipban} store and
 * they are kicked immediately. Reconnection from that address is refused by
 * {@link TempIpBanLoginListener} until the ban lapses. Durations use the compact form
 * parsed by {@link TimeSpans} ({@code 30m}, {@code 1h}, {@code 7d}, {@code 1d12h}, …).
 * Requires {@code sessentials.tempipban}.
 */
@SuppressWarnings("UnstableApiUsage")
final class TempIpBanCommand {

    private TempIpBanCommand() {
    }

    /**
     * Registers {@code /tempipban <player> <duration>}.
     *
     * @param plugin the owning plugin
     * @param reg    the Paper command registrar
     * @param bans   the shared temporary-ban registry
     */
    static void register(SEssentialsPlugin plugin, Commands reg, TempIpBans bans) {
        reg.register(Commands.literal("tempipban")
                .requires(s -> s.getSender().hasPermission("sessentials.tempipban"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(ctx -> ban(plugin, bans, ctx))))
                .build(), "Temporarily IP-ban an online player (e.g. 30m, 1h, 7d)");
    }

    /** Resolves the target's address, records the timed ban and kicks them. */
    private static int ban(SEssentialsPlugin plugin, TempIpBans bans, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        String durationText = StringArgumentType.getString(ctx, "duration");

        long durationMillis = TimeSpans.parseMillis(durationText);
        if (durationMillis <= 0) {
            Msg.err(sender, "Invalid duration '" + durationText + "' — use forms like 30m, 1h, 7d.");
            return 0;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            Msg.err(sender, name + " is not online.");
            return 0;
        }
        String ip = addressOf(target);
        if (ip == null) {
            Msg.err(sender, "Could not resolve " + target.getName() + "'s IP address.");
            return 0;
        }

        String human = TimeSpans.human(durationMillis);
        bans.add(ip, System.currentTimeMillis() + durationMillis, target.getName(), sender.getName());
        Component kickMessage = Component.text("You are temporarily IP-banned for " + human + ".");
        Schedulers.entity(plugin, target, () -> target.kick(kickMessage));
        Msg.ok(sender, "Temporarily IP-banned " + target.getName() + " (" + ip + ") for " + human + ".");
        return 1;
    }

    /**
     * @param player an online player
     * @return the player's raw host address, or {@code null} if it cannot be resolved
     */
    private static String addressOf(Player player) {
        InetSocketAddress socket = player.getAddress();
        if (socket == null || socket.getAddress() == null) {
            return null;
        }
        return socket.getAddress().getHostAddress();
    }
}
