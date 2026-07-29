package dev.iyanz.sessentials.module.ipinfo;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registers {@code /ipinfo <player>} and {@code /ipmatch <player>}: staff lookup
 * commands over the {@code "ipinfo"} data store maintained by {@link IpInfoListener}.
 */
@SuppressWarnings("UnstableApiUsage")
final class IpInfoCommands {

    private static final String KEY_IPS = ".ips";
    private static final String KEY_NAME = ".name";

    private IpInfoCommands() {
    }

    /**
     * Registers both commands against the plugin's command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("ipinfo")
                    .requires(s -> s.getSender().hasPermission("sessentials.ipinfo"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .executes(ctx -> ipInfo(plugin, ctx)))
                    .build(), "Show a player's recorded IP history");

            reg.register(Commands.literal("ipmatch")
                    .requires(s -> s.getSender().hasPermission("sessentials.ipinfo"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .executes(ctx -> ipMatch(plugin, ctx)))
                    .build(), "List other stored players that share an IP with a player");
        });
    }

    /** Reports a player's recorded IP history, plus their live IP if currently online. */
    @SuppressWarnings("deprecation")
    private static int ipInfo(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        String uuidKey = offline.getUniqueId().toString();

        YamlStore store = plugin.stores().get("ipinfo");
        List<String> ips = store.config().getStringList(uuidKey + KEY_IPS);
        String knownName = store.getString(uuidKey + KEY_NAME);

        if (ips.isEmpty() && knownName == null) {
            Msg.err(sender, "No IP history recorded for " + name + ".");
            return 0;
        }

        Msg.info(sender, "IP history for " + (knownName != null ? knownName : name) + ":");
        Msg.info(sender, ips.isEmpty() ? "No recorded IPs." : String.join(", ", ips));

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            InetSocketAddress address = online.getAddress();
            if (address != null && address.getAddress() != null) {
                Msg.value(sender, "Current IP:", address.getAddress().getHostAddress());
            }
        }
        return 1;
    }

    /** Lists other stored players sharing any recorded IP with the target (alt detection). */
    @SuppressWarnings("deprecation")
    private static int ipMatch(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        String targetKey = offline.getUniqueId().toString();

        YamlStore store = plugin.stores().get("ipinfo");
        List<String> targetIps = store.config().getStringList(targetKey + KEY_IPS);
        if (targetIps.isEmpty()) {
            Msg.err(sender, "No IP history recorded for " + name + ".");
            return 0;
        }

        List<String> matches = new ArrayList<>();
        for (String key : store.config().getKeys(false)) {
            if (key.equalsIgnoreCase(targetKey)) {
                continue;
            }
            List<String> otherIps = store.config().getStringList(key + KEY_IPS);
            if (shareAnyIp(targetIps, otherIps)) {
                matches.add(resolveName(key));
            }
        }

        if (matches.isEmpty()) {
            Msg.info(sender, "No matching IPs found for " + name + ".");
        } else {
            Msg.info(sender, "Shared IPs with " + name + ": " + String.join(", ", matches));
        }
        return 1;
    }

    /** @return {@code true} if any IP in {@code a} also appears in {@code b}. */
    private static boolean shareAnyIp(List<String> a, List<String> b) {
        for (String ip : a) {
            if (b.contains(ip)) {
                return true;
            }
        }
        return false;
    }

    /** Resolves a stored UUID key to a player name, falling back to the raw key. */
    private static String resolveName(String rawUuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(rawUuid));
            String resolvedName = player.getName();
            return resolvedName != null ? resolvedName : rawUuid;
        } catch (IllegalArgumentException ex) {
            return rawUuid;
        }
    }
}
