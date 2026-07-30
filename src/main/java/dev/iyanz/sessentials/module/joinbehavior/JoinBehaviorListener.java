package dev.iyanz.sessentials.module.joinbehavior;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Applies the configured every-join/quit behaviours: an optional teleport of each
 * joining player to spawn, and optional join/quit sounds played to everyone online.
 *
 * <p>Configuration ({@code join} section) is read once at construction and treated as
 * immutable for the lifetime of the listener. Sound keys are resolved eagerly against
 * {@link Registry#SOUNDS}; an unknown or malformed key is logged once and that sound
 * is simply skipped at runtime.</p>
 *
 * <p>Folia notes: {@link PlayerJoinEvent} is delivered on the joining player's region
 * thread, so {@link Player#teleportAsync} may be called directly. Sounds for other
 * players are always played after hopping to each recipient's own region thread via
 * {@link Schedulers#entity}.</p>
 */
final class JoinBehaviorListener implements Listener {

    /** Data store holding the shared spawn location (see {@code /setspawn}). */
    private static final String SPAWN_STORE = "spawn";

    /** Path of the spawn location within the {@link #SPAWN_STORE} store. */
    private static final String SPAWN_PATH = "spawn";

    /** Volume for join/quit sounds. */
    private static final float SOUND_VOLUME = 1.0f;

    /** Pitch for join/quit sounds. */
    private static final float SOUND_PITCH = 1.0f;

    private final SEssentialsPlugin plugin;
    private final boolean teleportSpawn;
    private final Sound joinSound;
    private final Sound quitSound;

    /**
     * Reads the {@code join} configuration once and resolves the configured sounds.
     *
     * @param plugin the owning plugin, used for config, stores and schedulers
     */
    JoinBehaviorListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.teleportSpawn = plugin.getConfig().getBoolean("join.teleport-spawn", false);
        boolean soundEnabled = plugin.getConfig().getBoolean("join.sound-enabled", false);
        this.joinSound = soundEnabled
                ? resolveSound(plugin.getConfig().getString("join.join-sound", "entity.player.levelup"))
                : null;
        this.quitSound = soundEnabled
                ? resolveSound(plugin.getConfig().getString("join.quit-sound", "block.note_block.bass"))
                : null;
    }

    /**
     * Handles every player join: teleports the player to spawn (if enabled) and plays
     * the join sound to all online players (if configured).
     *
     * @param event the join event, delivered on the joining player's region thread
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (teleportSpawn) {
            player.teleportAsync(spawnLocation(player), TeleportCause.PLUGIN);
        }
        if (joinSound != null) {
            playToAll(joinSound, null);
        }
    }

    /**
     * Handles every player quit: plays the quit sound to all remaining online players
     * (if configured). The quitting player is skipped.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (quitSound != null) {
            playToAll(quitSound, event.getPlayer());
        }
    }

    /**
     * Resolves the teleport destination: the shared spawn from the {@code spawn} data
     * store when one has been set, otherwise the joining player's world spawn.
     *
     * @param player the joining player, used for the world-spawn fallback
     * @return a non-null teleport destination
     */
    private Location spawnLocation(Player player) {
        Location spawn = plugin.stores().get(SPAWN_STORE).getLocation(SPAWN_PATH);
        return spawn != null ? spawn : player.getWorld().getSpawnLocation();
    }

    /**
     * Plays {@code sound} to every online player at their own location, hopping to each
     * recipient's region thread for Folia safety.
     *
     * @param sound the resolved sound to play
     * @param skip  a player to exclude (e.g. the quitting player), or {@code null}
     */
    private void playToAll(Sound sound, Player skip) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(skip)) {
                continue;
            }
            Schedulers.entity(plugin, online,
                    () -> online.playSound(online.getLocation(), sound, SOUND_VOLUME, SOUND_PITCH));
        }
    }

    /**
     * Resolves a configured sound key (e.g. {@code entity.player.levelup} or
     * {@code minecraft:block.note_block.bass}) against {@link Registry#SOUNDS}.
     * Null-safe at every step; an unresolvable key is logged as a warning.
     *
     * @param key the configured sound key, may be {@code null} or blank
     * @return the resolved sound, or {@code null} if the key is missing or unknown
     */
    private Sound resolveSound(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        NamespacedKey namespaced = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
        Sound sound = namespaced == null ? null : Registry.SOUNDS.get(namespaced);
        if (sound == null) {
            plugin.getLogger().warning("[joinbehavior] Unknown sound key '" + key + "' - sound disabled.");
        }
        return sound;
    }
}
