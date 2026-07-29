package dev.iyanz.sessentials.module.home;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Per-player homes: save, teleport to, delete and list named locations.
 *
 * <p>Commands: {@code /home [name]}, {@code /sethome [name]}, {@code /delhome [name]}
 * (all default to a home named {@code "home"} when the name is omitted), and
 * {@code /homes} (alias {@code /homelist}) to list the sender's homes. Home names are
 * case-insensitive; see {@link Homes} for the storage convention and
 * {@link Homes#maxHomes(SEssentialsPlugin, Player)} for the per-player home limit.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class HomeModule implements EssModule {

    private static final String DEFAULT_NAME = "home";

    @Override
    public String name() {
        return "home";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(
                    Commands.literal("home")
                            .requires(s -> s.getSender().hasPermission("sessentials.home"))
                            .executes(Cmds.playerExec(p -> teleportHome(plugin, p, DEFAULT_NAME)))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(HomeSuggestions.of(plugin))
                                    .executes(ctx -> {
                                        Player p = Cmds.player(ctx);
                                        if (p == null) {
                                            return 0;
                                        }
                                        teleportHome(plugin, p, StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    }))
                            .build(),
                    "Teleport to one of your homes");

            reg.register(
                    Commands.literal("sethome")
                            .requires(s -> s.getSender().hasPermission("sessentials.sethome"))
                            .executes(Cmds.playerExec(p -> setHome(plugin, p, DEFAULT_NAME)))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        Player p = Cmds.player(ctx);
                                        if (p == null) {
                                            return 0;
                                        }
                                        setHome(plugin, p, StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    }))
                            .build(),
                    "Save your current location as a home");

            reg.register(
                    Commands.literal("delhome")
                            .requires(s -> s.getSender().hasPermission("sessentials.delhome"))
                            .executes(Cmds.playerExec(p -> deleteHome(plugin, p, DEFAULT_NAME)))
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(HomeSuggestions.of(plugin))
                                    .executes(ctx -> {
                                        Player p = Cmds.player(ctx);
                                        if (p == null) {
                                            return 0;
                                        }
                                        deleteHome(plugin, p, StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    }))
                            .build(),
                    "Delete one of your homes");

            reg.register(
                    Commands.literal("homes")
                            .requires(s -> s.getSender().hasPermission("sessentials.homes"))
                            .executes(Cmds.playerExec(p -> listHomes(plugin, p)))
                            .build(),
                    "List your homes",
                    List.of("homelist"));
        });
    }

    /** Teleports {@code player} to their home {@code name}, or errors if it isn't set. */
    private static void teleportHome(SEssentialsPlugin plugin, Player player, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        Location location = Homes.location(plugin, player.getUniqueId(), lower);
        if (location == null) {
            Msg.err(player, "You have no home named \"" + lower + "\".");
            return;
        }
        player.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(player, "Teleported to home \"" + lower + "\".");
            } else {
                Msg.err(player, "Teleport to home \"" + lower + "\" failed.");
            }
        });
    }

    /**
     * Saves {@code player}'s current location as home {@code name}, enforcing the
     * per-player home limit ({@link Homes#maxHomes(SEssentialsPlugin, Player)}) when
     * creating a brand-new home (overwriting an existing home never counts against
     * the limit).
     */
    private static void setHome(SEssentialsPlugin plugin, Player player, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        boolean isNew = !Homes.exists(plugin, player.getUniqueId(), lower);
        if (isNew) {
            int max = Homes.maxHomes(plugin, player);
            int current = Homes.names(plugin, player.getUniqueId()).size();
            if (current >= max) {
                Msg.err(player, "You have reached your home limit (" + max + ").");
                return;
            }
        }
        Homes.save(plugin, player.getUniqueId(), lower, player.getLocation());
        Msg.ok(player, "Home \"" + lower + "\" set.");
    }

    /** Deletes {@code player}'s home {@code name}, or errors if it doesn't exist. */
    private static void deleteHome(SEssentialsPlugin plugin, Player player, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (!Homes.exists(plugin, player.getUniqueId(), lower)) {
            Msg.err(player, "You have no home named \"" + lower + "\".");
            return;
        }
        Homes.delete(plugin, player.getUniqueId(), lower);
        Msg.ok(player, "Home \"" + lower + "\" deleted.");
    }

    /** Lists {@code player}'s home names. */
    private static void listHomes(SEssentialsPlugin plugin, Player player) {
        List<String> names = Homes.names(plugin, player.getUniqueId());
        if (names.isEmpty()) {
            Msg.info(player, "You have no homes set.");
            return;
        }
        Msg.value(player, "Your homes:", String.join(", ", names));
    }
}
