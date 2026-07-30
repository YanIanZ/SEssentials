package dev.iyanz.sessentials.module.worldbehavior;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Read-only access to the shared spawn location managed by {@code /setspawn} (the
 * {@code spawn} data store, path {@code "spawn"}). This module never writes the spawn —
 * it only reads it to decide where to send players — so all access is funnelled through
 * these static helpers to keep that contract obvious.
 */
final class WorldSpawns {

    /** Data store holding the shared spawn location. */
    private static final String SPAWN_STORE = "spawn";

    /** Path of the spawn location within the {@link #SPAWN_STORE} store. */
    private static final String SPAWN_PATH = "spawn";

    private WorldSpawns() {
    }

    /**
     * @param plugin the owning plugin
     * @return the configured global spawn location, or {@code null} if none is set
     */
    static Location global(SEssentialsPlugin plugin) {
        return plugin.stores().get(SPAWN_STORE).getLocation(SPAWN_PATH);
    }

    /**
     * Resolves the spawn to teleport a player to when they enter {@code world}. Prefers
     * the configured global spawn when it lies inside {@code world} (so an admin's
     * {@code /setspawn} point wins), otherwise falls back to that world's own vanilla
     * spawn location.
     *
     * @param plugin the owning plugin
     * @param world  the world the player has just entered
     * @return the spawn location to teleport to (never {@code null})
     */
    static Location forWorld(SEssentialsPlugin plugin, World world) {
        Location global = global(plugin);
        if (global != null && world.equals(global.getWorld())) {
            return global;
        }
        return world.getSpawnLocation();
    }
}
