package dev.iyanz.sessentials.module.jail;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Jail system: named jail locations ({@code /setjail}, {@code /deljail}, {@code /jails})
 * and player confinement ({@code /jail}, {@code /unjail}).
 *
 * <p>A jailed player's state is persisted in the shared {@code jail.yml} store (see
 * {@link JailedPlayers}) so it survives restarts. While jailed, a player is teleported
 * back to their jail on join if their sentence hasn't expired, cannot teleport more
 * than a few blocks from the jail location, and cannot run most commands.</p>
 *
 * <p>Folia-safe: teleports use {@link Player#teleportAsync}, and any action on a
 * player other than the command sender hops to that player's region thread via
 * {@link Schedulers#entity}.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class JailModule implements EssModule, Listener {

    /** Commands a jailed player may still use (checked against the first whitespace-delimited token). */
    private static final Set<String> ALLOWED_WHILE_JAILED = Set.of("/msg", "/r", "/tell");

    /** Radius (in blocks) around a jail's location within which a jailed player may still teleport. */
    private static final double ALLOWED_RADIUS_SQUARED = 5.0 * 5.0;

    private SEssentialsPlugin plugin;

    @Override
    public String name() {
        return "jail";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.commands(reg -> {
            reg.register(Commands.literal("setjail")
                    .requires(s -> s.getSender().hasPermission("sessentials.setjail"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(this::setJail))
                    .build(), "Save your current location as a jail");

            reg.register(Commands.literal("deljail")
                    .requires(s -> s.getSender().hasPermission("sessentials.deljail"))
                    .then(Commands.argument("name", StringArgumentType.word()).suggests(JailSuggestions.of(plugin))
                            .executes(this::delJail))
                    .build(), "Delete a jail");

            reg.register(Commands.literal("jails")
                    .requires(s -> s.getSender().hasPermission("sessentials.jails"))
                    .executes(this::listJails)
                    .build(), "List all jail names");

            reg.register(Commands.literal("jail")
                    .requires(s -> s.getSender().hasPermission("sessentials.jail"))
                    .then(Commands.argument("player", StringArgumentType.word()).suggests(Cmds.PLAYERS)
                            .then(Commands.argument("jailName", StringArgumentType.word())
                                    .suggests(JailSuggestions.of(plugin))
                                    .executes(ctx -> jail(ctx, null))
                                    .then(Commands.argument("args", StringArgumentType.greedyString())
                                            .executes(ctx -> jail(ctx,
                                                    StringArgumentType.getString(ctx, "args"))))))
                    .build(), "Jail a player");

            reg.register(Commands.literal("unjail")
                    .requires(s -> s.getSender().hasPermission("sessentials.unjail"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .executes(this::unjail))
                    .build(), "Release a jailed player");
        });
    }

    // --- commands ------------------------------------------------------------

    private int setJail(CommandContext<CommandSourceStack> ctx) {
        Player p = Cmds.player(ctx);
        if (p == null) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        Jails.save(plugin, name, p.getLocation());
        Msg.ok(p, "Jail \"" + name + "\" set to your location.");
        return 1;
    }

    private int delJail(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        if (!Jails.exists(plugin, name)) {
            Msg.err(sender, "No jail named \"" + name + "\".");
            return 0;
        }
        Jails.delete(plugin, name);
        Msg.ok(sender, "Jail \"" + name + "\" deleted.");
        return 1;
    }

    private int listJails(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<String> names = Jails.names(plugin);
        if (names.isEmpty()) {
            Msg.info(sender, "There are no jails set.");
            return 1;
        }
        Msg.value(sender, "Jails:", String.join(", ", names));
        return 1;
    }

    /**
     * Jails the named online player in {@code jailName}. {@code args}, if present, is the
     * raw text following the jail name: its first whitespace-delimited token is tried as
     * a duration ({@link Durations#parse}); if it parses, the remainder (if any) is the
     * reason and the sentence expires after that duration, otherwise the whole of
     * {@code args} is treated as a permanent-sentence reason.
     */
    private int jail(CommandContext<CommandSourceStack> ctx, String args) {
        CommandSender sender = ctx.getSource().getSender();
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            Msg.err(sender, playerName + " is not online.");
            return 0;
        }

        String jailName = StringArgumentType.getString(ctx, "jailName").toLowerCase(Locale.ROOT);
        Location jailLocation = Jails.location(plugin, jailName);
        if (jailLocation == null) {
            Msg.err(sender, "No jail named \"" + jailName + "\".");
            return 0;
        }

        long until = 0L;
        String reason = null;
        if (args != null && !args.isBlank()) {
            String trimmed = args.trim();
            int space = trimmed.indexOf(' ');
            String firstToken = space < 0 ? trimmed : trimmed.substring(0, space);
            Duration duration = Durations.parse(firstToken);
            if (duration != null) {
                until = Instant.now().plus(duration).toEpochMilli();
                reason = space < 0 ? null : trimmed.substring(space + 1).trim();
            } else {
                reason = trimmed;
            }
            if (reason != null && reason.isBlank()) {
                reason = null;
            }
        }

        JailedPlayers.set(plugin, target.getUniqueId(), jailName, until, reason);

        long finalUntil = until;
        String finalReason = reason;
        Schedulers.entity(plugin, target, () -> target.teleportAsync(jailLocation).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.err(target, "You have been jailed" + (finalReason != null ? ": " + finalReason : "."));
            }
        }));

        Msg.ok(sender, "Jailed " + target.getName() + " in \"" + jailName + "\""
                + (finalUntil == 0L ? " permanently." : " for " + Durations.human(finalUntil - System.currentTimeMillis()) + ".")
                + (finalReason != null ? " Reason: " + finalReason : ""));
        return 1;
    }

    /** Releases a jailed player, clearing their state and teleporting them to spawn if online. */
    private int unjail(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerName = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();
        if (!JailedPlayers.isJailed(plugin, uuid)) {
            Msg.err(sender, playerName + " is not jailed.");
            return 0;
        }

        JailedPlayers.clear(plugin, uuid);
        Msg.ok(sender, "Unjailed " + playerName + ".");

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            Location spawn = spawnLocation(online);
            Schedulers.entity(plugin, online, () -> online.teleportAsync(spawn).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) {
                    Msg.ok(online, "You have been released.");
                }
            }));
        }
        return 1;
    }

    /** @return the configured global spawn, or {@code player}'s current world spawn if unset. */
    private Location spawnLocation(Player player) {
        Location spawn = plugin.stores().get("spawn").getLocation("spawn");
        return spawn != null ? spawn : player.getWorld().getSpawnLocation();
    }

    // --- listeners -------------------------------------------------------------

    /** Returns a jailed player to their jail on join, or clears their state if the sentence expired. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!JailedPlayers.isJailed(plugin, uuid)) {
            return;
        }
        long until = JailedPlayers.until(plugin, uuid);
        if (JailedPlayers.expired(until)) {
            JailedPlayers.clear(plugin, uuid);
            return;
        }
        String jailName = JailedPlayers.jailName(plugin, uuid);
        Location jailLocation = Jails.location(plugin, jailName);
        if (jailLocation == null) {
            return;
        }
        String reason = JailedPlayers.reason(plugin, uuid);
        Schedulers.entity(plugin, player, () -> {
            player.teleportAsync(jailLocation);
            Msg.err(player, "You are jailed" + (reason != null ? ": " + reason : ".")
                    + (until == 0L ? " (permanently)." : " (" + Durations.human(until - System.currentTimeMillis()) + " remaining)."));
        });
    }

    /**
     * Blocks a jailed player from teleporting away from their jail; the jailing/release
     * teleport itself is always allowed since it lands on (or clears) the jail location.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!JailedPlayers.isJailed(plugin, uuid)) {
            return;
        }
        long until = JailedPlayers.until(plugin, uuid);
        if (JailedPlayers.expired(until)) {
            JailedPlayers.clear(plugin, uuid);
            return;
        }
        String jailName = JailedPlayers.jailName(plugin, uuid);
        Location jailLocation = Jails.location(plugin, jailName);
        if (jailLocation == null) {
            return;
        }
        Location destination = event.getTo();
        if (destination == null || !withinJail(destination, jailLocation)) {
            event.setCancelled(true);
            Msg.err(player, "You cannot leave the jail.");
        }
    }

    /** @return whether {@code destination} is close enough to {@code jailLocation} to be allowed. */
    private static boolean withinJail(Location destination, Location jailLocation) {
        if (destination.getWorld() == null || jailLocation.getWorld() == null
                || !destination.getWorld().equals(jailLocation.getWorld())) {
            return false;
        }
        return destination.distanceSquared(jailLocation) <= ALLOWED_RADIUS_SQUARED;
    }

    /** Blocks a jailed player from running most commands, other than private messaging. */
    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!JailedPlayers.isJailed(plugin, uuid)) {
            return;
        }
        long until = JailedPlayers.until(plugin, uuid);
        if (JailedPlayers.expired(until)) {
            JailedPlayers.clear(plugin, uuid);
            return;
        }
        String label = event.getMessage().split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (ALLOWED_WHILE_JAILED.contains(label)) {
            return;
        }
        event.setCancelled(true);
        Msg.err(player, "You cannot use commands while jailed.");
    }
}
