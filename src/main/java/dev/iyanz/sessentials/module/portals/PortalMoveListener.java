package dev.iyanz.sessentials.module.portals;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Enforces portals: teleports a player who moves into a portal's bounding box to its
 * resolved destination.
 *
 * <p>To keep the very-hot move event cheap, work only happens when the player's block
 * position actually changed, and repeated triggers are suppressed with a short
 * per-player debounce so a player standing inside (or bouncing at the edge of) a
 * portal isn't teleported on every single tick. This event fires on the moving
 * player's own region thread, so {@link Player#teleportAsync} is called directly, with
 * no scheduler hop needed.</p>
 */
final class PortalMoveListener implements Listener {

    /** Minimum time between successive portal teleports for the same player. */
    private static final long DEBOUNCE_MILLIS = 2000L;

    private final SEssentialsPlugin plugin;
    private final PortalRegistry registry;
    private final ConcurrentHashMap<UUID, Long> lastTeleport = new ConcurrentHashMap<>();

    /**
     * @param plugin   the owning plugin, used to resolve a matched portal's destination
     * @param registry the portal registry consulted on every move
     */
    PortalMoveListener(SEssentialsPlugin plugin, PortalRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /** Teleports the mover to a matched portal's destination, subject to the debounce window. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to)) {
            return;
        }

        Portal portal = registry.find(to);
        if (portal == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastTeleport.get(uuid);
        if (last != null && now - last < DEBOUNCE_MILLIS) {
            return;
        }
        lastTeleport.put(uuid, now);

        Location destination = PortalDestinations.resolve(plugin, portal);
        if (destination == null) {
            Msg.err(player, "Portal \"" + portal.name() + "\" has no valid destination right now.");
            return;
        }
        player.teleportAsync(destination);
    }

    /** Drops the departing player's debounce entry so {@link #lastTeleport} doesn't leak across sessions. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTeleport.remove(event.getPlayer().getUniqueId());
    }

    /** @return whether {@code from} and {@code to} occupy the same block. */
    private static boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }
}
