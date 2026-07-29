package dev.iyanz.sessentials.module.portals;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Resolves a portal's stored destination token to an actual {@link Location}.
 *
 * <p>A destination is either the literal {@code "spawn"} (the global spawn store, or
 * the portal's own world spawn if unset) or a warp name, read directly from the warp
 * module's shared {@code warp.yml} store at {@code "warps.<name>"}. Resolution always
 * happens at teleport-time (never cached), so changing a warp's location or the global
 * spawn immediately affects every portal that points at it.</p>
 */
final class PortalDestinations {

    private static final String SPAWN_LITERAL = "spawn";
    private static final String SPAWN_STORE = "spawn";
    private static final String SPAWN_PATH = "spawn";
    private static final String WARP_STORE = "warp";

    private PortalDestinations() {
    }

    /**
     * @param plugin      the owning plugin (for store access)
     * @param destination the raw destination token (case-insensitive)
     * @return whether {@code destination} is the {@code "spawn"} literal or an existing warp name
     */
    static boolean isValid(SEssentialsPlugin plugin, String destination) {
        if (destination.equalsIgnoreCase(SPAWN_LITERAL)) {
            return true;
        }
        return plugin.stores().get(WARP_STORE).contains(warpPath(destination));
    }

    /**
     * @param plugin the owning plugin (for store access)
     * @param portal the portal whose destination to resolve
     * @return the resolved location, or {@code null} if the destination cannot be
     *         resolved (e.g. a warp that has since been deleted, or an unloaded world)
     */
    static Location resolve(SEssentialsPlugin plugin, Portal portal) {
        String destination = portal.destination();
        if (destination.equalsIgnoreCase(SPAWN_LITERAL)) {
            Location spawn = plugin.stores().get(SPAWN_STORE).getLocation(SPAWN_PATH);
            if (spawn != null) {
                return spawn;
            }
            World world = Bukkit.getWorld(portal.world());
            return world != null ? world.getSpawnLocation() : null;
        }
        return plugin.stores().get(WARP_STORE).getLocation(warpPath(destination));
    }

    private static String warpPath(String name) {
        return "warps." + name.toLowerCase(Locale.ROOT);
    }
}
