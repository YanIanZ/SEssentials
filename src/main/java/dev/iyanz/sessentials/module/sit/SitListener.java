package dev.iyanz.sessentials.module.sit;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up {@code /sit} seat entities the moment they stop being used: when the
 * seated player dismounts (sneak, teleport, the seat being killed, or mounting
 * something else) and, as a safety net, when the player disconnects.
 *
 * <p>Both events fire on the region thread that owns the player, and the actual seat
 * removal is delegated to {@link SitSeats}, which hops to the seat entity's own region
 * thread — so no thread-unsafe entity access happens here.</p>
 */
final class SitListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final SitSeats seats;

    /**
     * @param plugin the owning plugin, passed through for region-thread scheduling
     * @param seats  the shared seat registry to clean up
     */
    SitListener(SEssentialsPlugin plugin, SitSeats seats) {
        this.plugin = plugin;
        this.seats = seats;
    }

    /**
     * Removes the seat once its player dismounts. Runs at {@code MONITOR} priority and
     * ignores cancelled events so a dismount vetoed by another plugin keeps the seat.
     *
     * @param event the dismount event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            seats.handleDismount(plugin, player, event.getDismounted());
        }
    }

    /**
     * Sweeps the quitting player's seat so no orphaned stand or map entry lingers.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        seats.handleQuit(plugin, event.getPlayer().getUniqueId());
    }
}
