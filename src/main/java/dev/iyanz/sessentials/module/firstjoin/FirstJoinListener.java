package dev.iyanz.sessentials.module.firstjoin;

import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Fires the configured first-join actions when a player joins for the very first time.
 *
 * <p>A "first join" is detected with {@link Player#hasPlayedBefore()}: players the
 * server has never recorded before are the only ones handled; every subsequent join is
 * ignored. The configured actions (all optional and independent) are, in order:</p>
 * <ol>
 *   <li>Teleport to spawn — the {@code spawn} data store's {@code spawn} location, or
 *       the world's vanilla spawn as a fallback, applied on the player's region thread
 *       after a short settle delay.</li>
 *   <li>Broadcast a welcome message ({@link Msg#mm MiniMessage}, {@code %player%}
 *       replaced with the player's name) to every online player and the console.</li>
 *   <li>Run each configured command from the console, {@code %player%} substituted.</li>
 * </ol>
 *
 * <p>Config values are read once when the module enables and are treated as immutable
 * for the lifetime of the listener.</p>
 */
final class FirstJoinListener implements Listener {

    /** Data store holding the shared spawn location (see {@code /setspawn}). */
    private static final String SPAWN_STORE = "spawn";

    /** Path of the spawn location within the {@link #SPAWN_STORE} store. */
    private static final String SPAWN_PATH = "spawn";

    /** Ticks to wait before teleporting, letting the client finish loading in. */
    private static final long TELEPORT_DELAY_TICKS = 20L;

    private final SEssentialsPlugin plugin;
    private final boolean teleportToSpawn;
    private final String welcome;
    private final List<String> commands;

    /**
     * Reads the {@code first-join} configuration once.
     *
     * @param plugin the owning plugin, used for config, stores and schedulers
     */
    FirstJoinListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.teleportToSpawn = plugin.getConfig().getBoolean("first-join.teleport-to-spawn", true);
        this.welcome = plugin.getConfig().getString("first-join.welcome", "");
        this.commands = plugin.getConfig().getStringList("first-join.commands");
    }

    /** Runs the configured first-join actions for a genuinely new player. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        String name = player.getName();

        if (teleportToSpawn) {
            teleportToSpawn(player);
        }
        if (welcome != null && !welcome.isBlank()) {
            broadcastWelcome(name);
        }
        if (!commands.isEmpty()) {
            runCommands(name);
        }
    }

    /**
     * Schedules a spawn teleport on {@code player}'s region thread after a short delay,
     * resolving the store spawn (or the world spawn if unset) at teleport time.
     *
     * @param player the newly joined player
     */
    private void teleportToSpawn(Player player) {
        player.getScheduler().runDelayed(plugin, scheduled -> {
            YamlStore store = plugin.stores().get(SPAWN_STORE);
            Location spawn = store.getLocation(SPAWN_PATH);
            if (spawn == null) {
                spawn = player.getWorld().getSpawnLocation();
            }
            player.teleportAsync(spawn, TeleportCause.PLUGIN);
        }, null, TELEPORT_DELAY_TICKS);
    }

    /**
     * Sends the welcome message to the console and every online player, hopping to each
     * recipient's region thread for Folia safety.
     *
     * @param name the joining player's name, substituted for {@code %player%}
     */
    private void broadcastWelcome(String name) {
        Component message = Msg.mm(welcome.replace("%player%", name));
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, online, () -> online.sendMessage(message));
        }
    }

    /**
     * Dispatches each configured command from the console on the global region
     * scheduler, substituting {@code %player%} with the joining player's name.
     *
     * @param name the joining player's name
     */
    private void runCommands(String name) {
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw.replace("%player%", name);
            Bukkit.getGlobalRegionScheduler().execute(plugin,
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }
}
