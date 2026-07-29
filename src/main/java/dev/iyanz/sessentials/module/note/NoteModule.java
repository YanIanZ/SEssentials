package dev.iyanz.sessentials.module.note;

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

/**
 * Staff notes system: staff attach a private note to a player with {@code /note},
 * review a player's notes with {@code /notes}, remove a single entry with
 * {@code /delnote}, or wipe a player's notes with {@code /clearnotes}.
 *
 * <p>Notes are staff-facing only — the target player is never informed — and persist
 * across restarts in the {@code notes} data store; see {@link Notes} for the storage
 * format. All commands resolve their target via {@link Bukkit#getOfflinePlayer(String)}
 * so offline players can have notes written, listed, or cleaned up. Every command is
 * gated behind the single {@code sessentials.note} permission.</p>
 *
 * <p>The commands only read from and write to the shared data store, so they are
 * inherently Folia-safe: they touch no entity or world state and therefore need no
 * region-thread hop.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class NoteModule implements EssModule {

    private static final String PERMISSION = "sessentials.note";

    private YamlStore store;

    @Override
    public String name() {
        return "note";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.store = plugin.stores().get("notes");

        plugin.commands(reg -> {
            reg.register(Commands.literal("note")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                    .executes(this::add)))
                    .build(), "Add a staff note to a player");

            reg.register(Commands.literal("notes")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::list))
                    .build(), "List a player's staff notes");

            reg.register(Commands.literal("delnote")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                    .executes(this::delete)))
                    .build(), "Remove one of a player's staff notes");

            reg.register(Commands.literal("clearnotes")
                    .requires(s -> s.getSender().hasPermission(PERMISSION))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .executes(this::clear))
                    .build(), "Clear all of a player's staff notes");
        });
    }

    // --- commands ------------------------------------------------------------

    private int add(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        String text = StringArgumentType.getString(ctx, "text");

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        Notes.add(store, target.getUniqueId(), sender.getName(), text);
        Msg.ok(sender, "Added a note on " + name + ": " + text);
        return 1;
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        List<Notes.Entry> entries = Notes.list(store, target.getUniqueId());
        if (entries.isEmpty()) {
            Msg.info(sender, name + " has no notes.");
            return 1;
        }

        Msg.info(sender, name + "'s notes (" + entries.size() + "):");
        for (int i = 0; i < entries.size(); i++) {
            Notes.Entry entry = entries.get(i);
            String label = (i + 1) + ". " + entry.staffName() + " (" + Notes.relative(entry.epochMillis()) + " ago):";
            Msg.value(sender, label, entry.text());
        }
        return 1;
    }

    private int delete(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        int index = IntegerArgumentType.getInteger(ctx, "index");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        if (!Notes.removeAt(store, target.getUniqueId(), index - 1)) {
            Msg.err(sender, name + " has no note #" + index + ".");
            return 0;
        }
        Msg.ok(sender, "Removed note #" + index + " from " + name + ".");
        return 1;
    }

    private int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        int count = Notes.list(store, target.getUniqueId()).size();
        if (count == 0) {
            Msg.info(sender, name + " has no notes to clear.");
            return 1;
        }
        Notes.clear(store, target.getUniqueId());
        Msg.ok(sender, "Cleared " + count + " note" + (count == 1 ? "" : "s") + " from " + name + ".");
        return 1;
    }
}
