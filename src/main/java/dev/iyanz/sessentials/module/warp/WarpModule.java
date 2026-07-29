package dev.iyanz.sessentials.module.warp;

import java.util.List;
import java.util.Locale;

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
 * Global warps: named, server-wide locations any permitted player can teleport to.
 *
 * <p>Commands: {@code /warp <name>} (teleport to a warp), {@code /setwarp <name>}
 * (create or overwrite a warp at your current location), {@code /delwarp <name>}
 * (delete a warp) and {@code /warps} (alias {@code /warplist}) to list all warp
 * names. Warp names are case-insensitive; see {@link Warps} for the storage
 * convention.</p>
 *
 * <p>Phase 1 keeps warp permissions simple: {@code sessentials.warp} is sufficient
 * to teleport to any warp. A finer-grained {@code sessentials.warp.<name>}
 * "restricted warp" tier is intentionally not implemented yet (see this module's
 * return notes).</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class WarpModule implements EssModule {

    @Override
    public String name() {
        return "warp";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("warp")
                            .requires(s -> s.getSender().hasPermission("sessentials.warp"))
                            .executes(ctx -> {
                                Msg.err(ctx.getSource().getSender(), "Usage: /warp <name>.");
                                return 0;
                            })
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(WarpSuggestions.of(plugin))
                                    .executes(ctx -> {
                                        Player p = Cmds.player(ctx);
                                        if (p == null) {
                                            return 0;
                                        }
                                        warpTo(plugin, p, StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    }))
                            .build(),
                    "Teleport to a warp");

            reg.register(
                    Commands.literal("setwarp")
                            .requires(s -> s.getSender().hasPermission("sessentials.setwarp"))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        Player p = Cmds.player(ctx);
                                        if (p == null) {
                                            return 0;
                                        }
                                        setWarp(plugin, p, StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    }))
                            .build(),
                    "Create or overwrite a warp at your location");

            reg.register(
                    Commands.literal("delwarp")
                            .requires(s -> s.getSender().hasPermission("sessentials.delwarp"))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(WarpSuggestions.of(plugin))
                                    .executes(ctx -> {
                                        deleteWarp(plugin, ctx.getSource().getSender(),
                                                StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    }))
                            .build(),
                    "Delete a warp");

            reg.register(
                    Commands.literal("warps")
                            .requires(s -> s.getSender().hasPermission("sessentials.warps"))
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                if (sender instanceof Player player) {
                                    WarpsMenu.open(plugin, player);
                                } else {
                                    listWarps(plugin, sender);
                                }
                                return 1;
                            })
                            .build(),
                    "List all warps",
                    List.of("warplist"));
        });
    }

    /** Teleports {@code player} to the warp {@code name}, or errors if it isn't set. */
    private static void warpTo(SEssentialsPlugin plugin, Player player, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        Location location = Warps.location(plugin, lower);
        if (location == null) {
            Msg.err(player, "No warp named \"" + lower + "\".");
            return;
        }
        player.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(player, "Warped to \"" + lower + "\".");
            } else {
                Msg.err(player, "Teleport to warp \"" + lower + "\" failed.");
            }
        });
    }

    /**
     * Saves {@code player}'s current location as the warp {@code name}, creating it
     * or overwriting any existing warp of that name.
     */
    private static void setWarp(SEssentialsPlugin plugin, Player player, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        Warps.save(plugin, lower, player.getLocation());
        Msg.ok(player, "Warp \"" + lower + "\" set.");
    }

    /** Deletes the warp {@code name}, or errors if it doesn't exist. */
    private static void deleteWarp(SEssentialsPlugin plugin, CommandSender sender, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (!Warps.exists(plugin, lower)) {
            Msg.err(sender, "No warp named \"" + lower + "\".");
            return;
        }
        Warps.delete(plugin, lower);
        Msg.ok(sender, "Warp \"" + lower + "\" deleted.");
    }

    /** Lists all warp names. */
    private static void listWarps(SEssentialsPlugin plugin, CommandSender sender) {
        List<String> names = Warps.names(plugin);
        if (names.isEmpty()) {
            Msg.info(sender, "There are no warps set.");
            return;
        }
        Msg.value(sender, "Warps:", String.join(", ", names));
    }
}
