package dev.iyanz.sessentials.module.holograms;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Floating multi-line text holograms, rendered as a single {@link org.bukkit.entity.TextDisplay}
 * per hologram (Paper 1.21+).
 *
 * <p>Commands (all under {@code /hologram}, permission {@code sessentials.holograms}):</p>
 * <ul>
 *   <li>{@code /hologram create <id> <text...>} — create a one-line hologram at the
 *       sender's location.</li>
 *   <li>{@code /hologram delete <id>} — remove a hologram (persisted data + entity).</li>
 *   <li>{@code /hologram list} — list every hologram id.</li>
 *   <li>{@code /hologram addline <id> <text...>} — append a line.</li>
 *   <li>{@code /hologram setline <id> <n> <text...>} — replace line {@code n} (1-based).</li>
 *   <li>{@code /hologram removeline <id> <n>} — remove line {@code n} (1-based); a
 *       hologram's last remaining line cannot be removed this way (delete it instead).</li>
 *   <li>{@code /hologram movehere <id>} — move a hologram to the sender's location.</li>
 *   <li>{@code /hologram tp <id>} — teleport the sender to a hologram.</li>
 * </ul>
 *
 * <p>Hologram ids are case-insensitive (stored lower-case), like warp and home names.
 * See {@link Holograms} for the storage convention and {@link HologramRenderer} for
 * the Folia-safe entity lifecycle (spawn-on-enable cleanup, per-region dispatch).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class HologramsModule implements EssModule {

    private static final String PERMISSION = "sessentials.holograms";

    @Override
    public String name() {
        return "holograms";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        HologramRenderer.loadAll(plugin);

        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("hologram")
                            .requires(s -> s.getSender().hasPermission(PERMISSION))
                            .executes(ctx -> {
                                Msg.err(ctx.getSource().getSender(),
                                        "Usage: /hologram <create|delete|list|addline|setline|removeline|movehere|tp>.");
                                return 0;
                            })
                            .then(Commands.literal("create")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        Player p = Cmds.player(ctx);
                                                        if (p == null) {
                                                            return 0;
                                                        }
                                                        create(plugin, p, StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "text"));
                                                        return 1;
                                                    }))))
                            .then(Commands.literal("delete")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .suggests(HologramSuggestions.of(plugin))
                                            .executes(ctx -> {
                                                delete(plugin, ctx.getSource().getSender(),
                                                        StringArgumentType.getString(ctx, "id"));
                                                return 1;
                                            })))
                            .then(Commands.literal("list")
                                    .executes(ctx -> {
                                        list(plugin, ctx.getSource().getSender());
                                        return 1;
                                    }))
                            .then(Commands.literal("addline")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .suggests(HologramSuggestions.of(plugin))
                                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        addLine(plugin, ctx.getSource().getSender(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "text"));
                                                        return 1;
                                                    }))))
                            .then(Commands.literal("setline")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .suggests(HologramSuggestions.of(plugin))
                                            .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                                    .then(Commands.argument("text", StringArgumentType.greedyString())
                                                            .executes(ctx -> {
                                                                setLine(plugin, ctx.getSource().getSender(),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        IntegerArgumentType.getInteger(ctx, "n"),
                                                                        StringArgumentType.getString(ctx, "text"));
                                                                return 1;
                                                            })))))
                            .then(Commands.literal("removeline")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .suggests(HologramSuggestions.of(plugin))
                                            .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        removeLine(plugin, ctx.getSource().getSender(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                IntegerArgumentType.getInteger(ctx, "n"));
                                                        return 1;
                                                    }))))
                            .then(Commands.literal("movehere")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .suggests(HologramSuggestions.of(plugin))
                                            .executes(ctx -> {
                                                Player p = Cmds.player(ctx);
                                                if (p == null) {
                                                    return 0;
                                                }
                                                moveHere(plugin, p, StringArgumentType.getString(ctx, "id"));
                                                return 1;
                                            })))
                            .then(Commands.literal("tp")
                                    .then(Commands.argument("id", StringArgumentType.word())
                                            .suggests(HologramSuggestions.of(plugin))
                                            .executes(ctx -> {
                                                Player p = Cmds.player(ctx);
                                                if (p == null) {
                                                    return 0;
                                                }
                                                teleportTo(plugin, p, StringArgumentType.getString(ctx, "id"));
                                                return 1;
                                            })))
                            .build(),
                    "Create and manage floating text holograms");
        });
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        HologramRenderer.unloadAll(plugin);
    }

    /** Creates a new one-line hologram at {@code player}'s location, or errors if {@code id} is taken. */
    private static void create(SEssentialsPlugin plugin, Player player, String id, String text) {
        String key = Holograms.key(id);
        if (Holograms.exists(plugin, key)) {
            Msg.err(player, "A hologram named \"" + key + "\" already exists.");
            return;
        }
        Location location = player.getLocation();
        Holograms.create(plugin, key, location, text);
        HologramRenderer.spawn(plugin, key, location, List.of(text));
        Msg.ok(player, "Hologram \"" + key + "\" created.");
    }

    /** Deletes hologram {@code id}'s persisted data and its live entity, or errors if it doesn't exist. */
    private static void delete(SEssentialsPlugin plugin, CommandSender sender, String id) {
        String key = Holograms.key(id);
        Location location = Holograms.location(plugin, key);
        if (location == null) {
            Msg.err(sender, "No hologram named \"" + key + "\".");
            return;
        }
        HologramRenderer.despawn(plugin, key, location);
        Holograms.delete(plugin, key);
        Msg.ok(sender, "Hologram \"" + key + "\" deleted.");
    }

    /** Lists every hologram id. */
    private static void list(SEssentialsPlugin plugin, CommandSender sender) {
        List<String> ids = Holograms.ids(plugin);
        if (ids.isEmpty()) {
            Msg.info(sender, "There are no holograms set.");
            return;
        }
        Msg.value(sender, "Holograms:", String.join(", ", ids));
    }

    /** Appends {@code text} as a new line on hologram {@code id}, or errors if it doesn't exist. */
    private static void addLine(SEssentialsPlugin plugin, CommandSender sender, String id, String text) {
        String key = Holograms.key(id);
        Location location = Holograms.location(plugin, key);
        if (location == null) {
            Msg.err(sender, "No hologram named \"" + key + "\".");
            return;
        }
        List<String> lines = new ArrayList<>(Holograms.lines(plugin, key));
        lines.add(text);
        Holograms.setLines(plugin, key, lines);
        HologramRenderer.updateText(plugin, key, location, lines);
        Msg.ok(sender, "Line added to hologram \"" + key + "\" (now " + lines.size() + " lines).");
    }

    /** Replaces 1-based line {@code n} of hologram {@code id} with {@code text}. */
    private static void setLine(SEssentialsPlugin plugin, CommandSender sender, String id, int n, String text) {
        String key = Holograms.key(id);
        Location location = Holograms.location(plugin, key);
        if (location == null) {
            Msg.err(sender, "No hologram named \"" + key + "\".");
            return;
        }
        List<String> lines = new ArrayList<>(Holograms.lines(plugin, key));
        if (n < 1 || n > lines.size()) {
            Msg.err(sender, "Hologram \"" + key + "\" has no line " + n + " (it has " + lines.size() + ").");
            return;
        }
        lines.set(n - 1, text);
        Holograms.setLines(plugin, key, lines);
        HologramRenderer.updateText(plugin, key, location, lines);
        Msg.ok(sender, "Line " + n + " of hologram \"" + key + "\" updated.");
    }

    /** Removes 1-based line {@code n} of hologram {@code id}; refuses to remove the only remaining line. */
    private static void removeLine(SEssentialsPlugin plugin, CommandSender sender, String id, int n) {
        String key = Holograms.key(id);
        Location location = Holograms.location(plugin, key);
        if (location == null) {
            Msg.err(sender, "No hologram named \"" + key + "\".");
            return;
        }
        List<String> lines = new ArrayList<>(Holograms.lines(plugin, key));
        if (n < 1 || n > lines.size()) {
            Msg.err(sender, "Hologram \"" + key + "\" has no line " + n + " (it has " + lines.size() + ").");
            return;
        }
        if (lines.size() == 1) {
            Msg.err(sender, "Cannot remove the only line of \"" + key + "\"; delete the hologram instead.");
            return;
        }
        lines.remove(n - 1);
        Holograms.setLines(plugin, key, lines);
        HologramRenderer.updateText(plugin, key, location, lines);
        Msg.ok(sender, "Line " + n + " removed from hologram \"" + key + "\".");
    }

    /** Moves hologram {@code id} to {@code player}'s location, or errors if it doesn't exist. */
    private static void moveHere(SEssentialsPlugin plugin, Player player, String id) {
        String key = Holograms.key(id);
        Location oldLocation = Holograms.location(plugin, key);
        if (oldLocation == null) {
            Msg.err(player, "No hologram named \"" + key + "\".");
            return;
        }
        Location newLocation = player.getLocation();
        Holograms.setLocation(plugin, key, newLocation);
        HologramRenderer.relocate(plugin, key, oldLocation, newLocation, Holograms.lines(plugin, key));
        Msg.ok(player, "Hologram \"" + key + "\" moved to your location.");
    }

    /** Teleports {@code player} to hologram {@code id}, or errors if it doesn't exist. */
    private static void teleportTo(SEssentialsPlugin plugin, Player player, String id) {
        String key = Holograms.key(id);
        Location location = Holograms.location(plugin, key);
        if (location == null) {
            Msg.err(player, "No hologram named \"" + key + "\".");
            return;
        }
        player.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(player, "Teleported to hologram \"" + key + "\".");
            } else {
                Msg.err(player, "Teleport to hologram \"" + key + "\" failed.");
            }
        });
    }
}
