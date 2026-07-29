package dev.iyanz.sessentials.module.ctext;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
 * Registers and handles the {@code /ctext}, {@code /ctextall} and {@code /ctextlist}
 * commands, which replay operator-defined rich messages read via {@link CTexts}.
 *
 * <p>All three share the {@code sessentials.ctext} permission. Message bodies are
 * MiniMessage authored by staff, so they render verbatim (see {@link CTextModule}).
 * Delivery is Folia-safe: every recipient is messaged on its own region thread via
 * {@link Schedulers#entity}, and plain-text status feedback to the invoker uses the
 * small-caps {@link Msg} helpers.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class CTextCommands {

    /** Shared permission for every ctext command. */
    private static final String PERMISSION = "sessentials.ctext";

    private CTextCommands() {
    }

    /**
     * Registers {@code /ctext} (with its {@code reload}, {@code <player>} and
     * {@code all} forms), {@code /ctextall <name>} and {@code /ctextlist}.
     *
     * @param plugin the owning plugin
     * @param ctexts the config-backed ctext source
     */
    static void register(SEssentialsPlugin plugin, CTexts ctexts) {
        SuggestionProvider<CommandSourceStack> nameSuggestions = (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String name : ctexts.names()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };

        plugin.commands(reg -> {
            reg.register(Commands.literal("ctext")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.literal("reload")
                            .executes(ctx -> handleReload(plugin, ctx)))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(nameSuggestions)
                            .executes(ctx -> handleSelf(ctexts, ctx))
                            .then(Commands.literal("all")
                                    .executes(ctx -> handleAll(plugin, ctexts, ctx)))
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests(Cmds.PLAYERS)
                                    .executes(ctx -> handleTarget(plugin, ctexts, ctx))))
                    .build(), "Send an operator-defined rich message", List.of());

            reg.register(Commands.literal("ctextall")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(nameSuggestions)
                            .executes(ctx -> handleAll(plugin, ctexts, ctx)))
                    .build(), "Broadcast an operator-defined rich message to everyone", List.of());

            reg.register(Commands.literal("ctextlist")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .executes(ctx -> handleList(ctexts, ctx))
                    .build(), "List the defined ctext names", List.of());
        });
    }

    /** Sends the named ctext to the command's sender. */
    private static int handleSelf(CTexts ctexts, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        List<Component> lines = ctexts.render(name);
        if (lines.isEmpty()) {
            Msg.err(sender, "No ctext named " + name + ".");
            return 0;
        }
        lines.forEach(sender::sendMessage);
        return 1;
    }

    /** Sends the named ctext to a single online player, on that player's region thread. */
    private static int handleTarget(SEssentialsPlugin plugin, CTexts ctexts, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        List<Component> lines = ctexts.render(name);
        if (lines.isEmpty()) {
            Msg.err(sender, "No ctext named " + name + ".");
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "target");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Msg.err(sender, targetName + " is not online.");
            return 0;
        }
        Schedulers.entity(plugin, target, () -> lines.forEach(target::sendMessage));
        Msg.ok(sender, "Sent " + name + " to " + target.getName() + ".");
        return 1;
    }

    /** Broadcasts the named ctext to the console and every online player (each on its region thread). */
    private static int handleAll(SEssentialsPlugin plugin, CTexts ctexts, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        List<Component> lines = ctexts.render(name);
        if (lines.isEmpty()) {
            Msg.err(sender, "No ctext named " + name + ".");
            return 0;
        }
        lines.forEach(Bukkit.getConsoleSender()::sendMessage);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> lines.forEach(player::sendMessage));
        }
        Msg.ok(sender, "Broadcast " + name + " to everyone.");
        return 1;
    }

    /** Lists the defined ctext names to the sender. */
    private static int handleList(CTexts ctexts, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> names = ctexts.names();
        if (names.isEmpty()) {
            Msg.info(sender, "No ctexts are defined.");
            return 1;
        }
        Msg.value(sender, "Ctexts (" + names.size() + "):", String.join(", ", names));
        return 1;
    }

    /** Reloads the plugin config so subsequent ctext lookups see edited values. */
    private static int handleReload(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        plugin.reloadConfig();
        Msg.ok(ctx.getSource().getSender(), "Ctext config reloaded.");
        return 1;
    }
}
