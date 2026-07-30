package dev.iyanz.sessentials.module.worldbehavior;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Applies the configured custom-respawn behaviours when a player respawns:
 * <ol>
 *   <li><b>Respawn at spawn</b> — if {@code respawn.at-spawn} is enabled and a global
 *       spawn is set, the respawn location is redirected there via
 *       {@link PlayerRespawnEvent#setRespawnLocation(Location)}. A player's own bed /
 *       respawn anchor is left untouched only when no global spawn is configured.</li>
 *   <li><b>Respawn commands</b> — every entry of {@code respawn.commands} is dispatched
 *       from the console with {@code {player}} substituted for the player's name.</li>
 * </ol>
 *
 * <p>Vanilla already restores the respawning player's health and food, so this listener
 * deliberately does not re-heal. Setting the respawn location on the event is the
 * supported, Folia-safe way to reposition a respawn (no teleport is issued). Console
 * commands run on the global region scheduler.</p>
 */
final class RespawnListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final WorldBehaviorSettings settings;

    RespawnListener(SEssentialsPlugin plugin, WorldBehaviorSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** Redirects the respawn location and/or runs the configured respawn commands. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (settings.respawnAtSpawn()) {
            Location spawn = WorldSpawns.global(plugin);
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
        }

        runRespawnCommands(player.getName());
    }

    /**
     * Dispatches each configured respawn command from the console on the global region
     * scheduler, substituting {@code {player}} with the respawning player's name.
     *
     * @param name the respawning player's name
     */
    private void runRespawnCommands(String name) {
        for (String raw : settings.respawnCommands()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw.replace("{player}", name);
            Bukkit.getGlobalRegionScheduler().execute(plugin,
                    () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }
}
