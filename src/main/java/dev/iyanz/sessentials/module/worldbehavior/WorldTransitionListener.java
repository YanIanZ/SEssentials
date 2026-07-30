package dev.iyanz.sessentials.module.worldbehavior;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Applies the passive, per-world behaviours whenever a player enters a world:
 * <ol>
 *   <li><b>Forced game mode</b> — if {@code world-gamemode.&lt;world&gt;} names a mode,
 *       the player's game mode is set to it. Applied both on {@link PlayerJoinEvent}
 *       (the login world) and on {@link PlayerChangedWorldEvent} (every later
 *       transition).</li>
 *   <li><b>Teleport to world spawn</b> — if {@code world-behavior.spawn-on-change} is
 *       enabled, the player is teleported to the new world's spawn on
 *       {@link PlayerChangedWorldEvent}.</li>
 * </ol>
 *
 * <p>Folia safety: both actions mutate the player, so they run on the player's own
 * region thread via {@link Schedulers#entity}. Deferring the world-change teleport to the
 * next tick also keeps it from fighting the teleport that triggered the world change;
 * the reposition always uses {@link Player#teleportAsync(Location, TeleportCause)}.</p>
 */
final class WorldTransitionListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final WorldBehaviorSettings settings;

    WorldTransitionListener(SEssentialsPlugin plugin, WorldBehaviorSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** Applies the forced game mode for the login world, if one is configured. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyForcedGameMode(event.getPlayer());
    }

    /**
     * Applies the forced game mode for the new world and, when enabled, teleports the
     * player to that world's spawn.
     */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        applyForcedGameMode(player);
        if (settings.spawnOnChange()) {
            teleportToWorldSpawn(player);
        }
    }

    /**
     * Sets the player's game mode to the one configured for their current world, if any,
     * on the player's region thread.
     *
     * @param player the player to adjust
     */
    private void applyForcedGameMode(Player player) {
        if (!settings.hasForcedGameModes()) {
            return;
        }
        GameMode mode = settings.forcedGameMode(player.getWorld().getName());
        if (mode == null) {
            return;
        }
        Schedulers.entity(plugin, player, () -> {
            if (player.getGameMode() != mode) {
                player.setGameMode(mode);
            }
        });
    }

    /**
     * Teleports the player to the spawn of the world they have just entered, deferred to
     * the next tick on the player's region thread so it does not race the teleport that
     * caused the world change.
     *
     * @param player the player who changed worlds
     */
    private void teleportToWorldSpawn(Player player) {
        Schedulers.entity(plugin, player, () -> {
            Location target = WorldSpawns.forWorld(plugin, player.getWorld());
            player.teleportAsync(target, TeleportCause.PLUGIN);
        });
    }
}
