package dev.iyanz.sessentials.module.ignore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Chat-ignore toggling: {@code /ignore <player>} adds or removes a player from
 * your personal ignore list, and {@code /ignorelist} shows who is on it.
 *
 * <p>Ignore lists are persisted per-player in the {@code "ignore"} data store as a
 * list of lowercased target UUIDs under {@code "<uuid>.ignored"}. Other modules
 * (e.g. private messaging) may consult {@link #ignores(SEssentialsPlugin, UUID, UUID)}
 * to decide whether to deliver something from one player to another; this module
 * only toggles/lists the relationship and does not itself gate any delivery.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class IgnoreModule implements EssModule {

    private static final String IGNORED_SUFFIX = ".ignored";

    @Override
    public String name() {
        return "ignore";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("ignore")
                    .requires(s -> s.getSender().hasPermission("sessentials.ignore"))
                    .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(Cmds.PLAYERS)
                            .executes(ctx -> handleToggle(plugin, ctx)))
                    .build(), "Toggle ignoring a player");

            reg.register(Commands.literal("ignorelist")
                    .requires(s -> s.getSender().hasPermission("sessentials.ignore"))
                    .executes(Cmds.playerExec(p -> handleList(plugin, p)))
                    .build(), "List the players you are ignoring");
        });
    }

    @SuppressWarnings("deprecation")
    private static int handleToggle(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player sender = Cmds.player(ctx);
        if (sender == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "target");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            Msg.err(sender, "You can't ignore yourself.");
            return 0;
        }

        YamlStore store = plugin.stores().get("ignore");
        String path = sender.getUniqueId() + IGNORED_SUFFIX;
        List<String> ignored = new ArrayList<>(store.config().getStringList(path));
        String targetId = target.getUniqueId().toString().toLowerCase(Locale.ROOT);

        boolean wasIgnored = ignored.removeIf(id -> id.equalsIgnoreCase(targetId));
        if (!wasIgnored) {
            ignored.add(targetId);
        }
        store.set(path, ignored);
        store.save();

        if (wasIgnored) {
            Msg.ok(sender, "You are no longer ignoring " + name + ".");
        } else {
            Msg.ok(sender, "You are now ignoring " + name + ".");
        }
        return 1;
    }

    private static void handleList(SEssentialsPlugin plugin, Player sender) {
        YamlStore store = plugin.stores().get("ignore");
        List<String> ignored = store.config().getStringList(sender.getUniqueId() + IGNORED_SUFFIX);
        if (ignored.isEmpty()) {
            Msg.info(sender, "You are not ignoring anyone.");
            return;
        }

        List<String> names = new ArrayList<>(ignored.size());
        for (String raw : ignored) {
            names.add(resolveName(raw));
        }
        Msg.info(sender, "Ignoring: " + String.join(", ", names));
    }

    private static String resolveName(String rawUuid) {
        try {
            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(rawUuid));
            String name = target.getName();
            return name != null ? name : rawUuid;
        } catch (IllegalArgumentException ex) {
            return rawUuid;
        }
    }

    /**
     * Consults the {@code "ignore"} data store directly, so other modules can check
     * an ignore relationship without holding a reference to this module.
     *
     * @param plugin the owning plugin, used to reach the ignore data store
     * @param who    the player whose ignore list is checked
     * @param target the player being checked for
     * @return {@code true} if {@code who} is currently ignoring {@code target}
     */
    public static boolean ignores(SEssentialsPlugin plugin, UUID who, UUID target) {
        YamlStore store = plugin.stores().get("ignore");
        List<String> ignored = store.config().getStringList(who + IGNORED_SUFFIX);
        String targetId = target.toString();
        for (String id : ignored) {
            if (id.equalsIgnoreCase(targetId)) {
                return true;
            }
        }
        return false;
    }
}
