package dev.iyanz.sessentials.module.warnings;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Warning system: staff issue a warning with {@code /warn}, review a player's history
 * with {@code /warnings} (alias {@code /warns}), remove a single entry with
 * {@code /delwarn}, or wipe a player's history with {@code /clearwarnings} (alias
 * {@code /clearwarns}).
 *
 * <p>Warnings persist across restarts in the {@code warnings} data store; see
 * {@link Warnings} for the storage format. All commands resolve their target via
 * {@link Bukkit#getOfflinePlayer(String)} so offline players can be warned, listed,
 * or have their warnings cleaned up.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class WarningsModule implements EssModule {

    private YamlStore store;

    @Override
    public String name() {
        return "warnings";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.store = plugin.stores().get("warnings");

        plugin.commands(reg -> {
            reg.register(Commands.literal("warn")
                    .requires(s -> s.getSender().hasPermission("sessentials.warn"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                    .executes(this::warn)))
                    .build(), "Warn a player");

            reg.register(Commands.literal("warnings")
                    .requires(s -> s.getSender().hasPermission("sessentials.warnings"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::list))
                    .build(), "List a player's warnings", List.of("warns"));

            reg.register(Commands.literal("delwarn")
                    .requires(s -> s.getSender().hasPermission("sessentials.delwarn"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                    .executes(this::delete)))
                    .build(), "Remove one of a player's warnings");

            reg.register(Commands.literal("clearwarnings")
                    .requires(s -> s.getSender().hasPermission("sessentials.clearwarnings"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::clear))
                    .build(), "Clear all of a player's warnings", List.of("clearwarns"));
        });
    }

    // --- commands ------------------------------------------------------------

    private int warn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        String reason = StringArgumentType.getString(ctx, "reason");

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        Warnings.add(store, target.getUniqueId(), sender.getName(), reason);
        Msg.ok(sender, "Warned " + name + ": " + reason);

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            Msg.err(online, "You have been warned: " + reason);
        }
        return 1;
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        List<Warnings.Entry> entries = Warnings.list(store, target.getUniqueId());
        if (entries.isEmpty()) {
            Msg.info(sender, name + " has no warnings.");
            return 1;
        }

        Msg.info(sender, name + "'s warnings (" + entries.size() + "):");
        for (int i = 0; i < entries.size(); i++) {
            Warnings.Entry entry = entries.get(i);
            String label = (i + 1) + ". " + entry.staffName() + " (" + Warnings.relative(entry.epochMillis()) + " ago):";
            Msg.value(sender, label, entry.reason());
        }
        return 1;
    }

    private int delete(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        int index = IntegerArgumentType.getInteger(ctx, "index");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        if (!Warnings.removeAt(store, target.getUniqueId(), index - 1)) {
            Msg.err(sender, name + " has no warning #" + index + ".");
            return 0;
        }
        Msg.ok(sender, "Removed warning #" + index + " from " + name + ".");
        return 1;
    }

    private int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        int count = Warnings.list(store, target.getUniqueId()).size();
        if (count == 0) {
            Msg.info(sender, name + " has no warnings to clear.");
            return 1;
        }
        Warnings.clear(store, target.getUniqueId());
        Msg.ok(sender, "Cleared " + count + " warning" + (count == 1 ? "" : "s") + " from " + name + ".");
        return 1;
    }
}
