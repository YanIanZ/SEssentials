package dev.iyanz.sessentials.module.ipban;

import java.net.InetSocketAddress;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * IP-ban administration: ban an online player's address (kicking everyone who shares
 * it), ban a raw address, lift a ban, and list active bans. Every command is gated on
 * the single {@code sessentials.ipban} admin permission.
 *
 * <p>Bans are enforced at connection time by {@link IpBanLoginListener} against the
 * {@link BannedIps} registry, so a banned player cannot rejoin. All persistence lives
 * in the {@code ipban} store; the login listener only ever reads the in-memory
 * snapshot, keeping the async login thread off the YAML file (Folia-safe).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class IpBanModule implements EssModule {

    /** Permission required for every command in this module. */
    private static final String PERM = "sessentials.ipban";

    /** Message shown to a player when they are kicked or refused for an IP ban. */
    private static final Component KICK_MESSAGE = Component.text("You are IP-banned.");

    private SEssentialsPlugin plugin;
    private BannedIps banned;

    @Override
    public String name() {
        return "ipban";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.banned = new BannedIps(plugin.stores().get("ipban"));
        plugin.getServer().getPluginManager().registerEvents(new IpBanLoginListener(banned), plugin);

        plugin.commands(reg -> {
            reg.register(Commands.literal("ipban")
                    .requires(s -> s.getSender().hasPermission(PERM))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::ipBanPlayer))
                    .build(), "IP-ban an online player and kick anyone sharing their IP");

            reg.register(Commands.literal("ipban-ip")
                    .requires(s -> s.getSender().hasPermission(PERM))
                    .then(Commands.argument("ip", StringArgumentType.word())
                            .executes(this::ipBanRaw))
                    .build(), "Ban a raw IP address");

            reg.register(Commands.literal("ipunban")
                    .requires(s -> s.getSender().hasPermission(PERM))
                    .then(Commands.argument("ip", StringArgumentType.word())
                            .executes(this::ipUnban))
                    .build(), "Remove a banned IP address");

            reg.register(Commands.literal("ipbanlist")
                    .requires(s -> s.getSender().hasPermission(PERM))
                    .executes(this::ipBanList)
                    .build(), "List banned IP addresses");
        });
    }

    // --- commands ----------------------------------------------------------

    /** Bans the resolved IP of an online player, then kicks everyone on that IP. */
    private int ipBanPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
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
        boolean added = banned.add(ip, sender.getName());
        int kicked = kickSharing(ip);
        if (added) {
            Msg.ok(sender, "IP-banned " + target.getName() + " (" + ip + "). Kicked " + kicked + " player(s).");
        } else {
            Msg.info(sender, ip + " was already banned. Kicked " + kicked + " player(s).");
        }
        return 1;
    }

    /** Bans a raw address string supplied by the sender. */
    private int ipBanRaw(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String ip = StringArgumentType.getString(ctx, "ip");
        boolean added = banned.add(ip, sender.getName());
        int kicked = kickSharing(ip);
        if (added) {
            Msg.ok(sender, "IP-banned " + ip + (kicked > 0 ? ". Kicked " + kicked + " player(s)." : "."));
        } else {
            Msg.info(sender, ip + " is already banned.");
        }
        return 1;
    }

    /** Lifts the ban on a raw address string. */
    private int ipUnban(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String ip = StringArgumentType.getString(ctx, "ip");
        if (banned.remove(ip)) {
            Msg.ok(sender, "Removed the IP ban for " + ip + ".");
        } else {
            Msg.err(sender, ip + " is not banned.");
        }
        return 1;
    }

    /** Lists every banned address. */
    private int ipBanList(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> ips = banned.list();
        if (ips.isEmpty()) {
            Msg.info(sender, "No IP addresses are banned.");
            return 1;
        }
        Msg.info(sender, "Banned IPs (" + ips.size() + "):");
        for (String ip : ips) {
            Msg.raw(sender, Style.GRAY + "• " + Style.VALUE + ip);
        }
        return 1;
    }

    // --- helpers -----------------------------------------------------------

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

    /**
     * Kicks every online player currently connected from {@code ip}. Each kick hops to
     * that player's own region thread (Folia requirement for entity API).
     *
     * @param ip the banned host address
     * @return the number of players kicked
     */
    private int kickSharing(String ip) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (ip.equals(addressOf(p))) {
                Schedulers.entity(plugin, p, () -> p.kick(KICK_MESSAGE));
                count++;
            }
        }
        return count;
    }
}
