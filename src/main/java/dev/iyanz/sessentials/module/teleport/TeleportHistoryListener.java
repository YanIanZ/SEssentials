package dev.iyanz.sessentials.module.teleport;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Feeds {@link TeleportHistory} so {@code /back} works: remembers where a player
 * teleported <em>from</em> before every teleport, and where they died. Both events
 * fire on the affected player's own region thread, so reading their location here is
 * Folia-safe without any extra scheduler hop.
 */
final class TeleportHistoryListener implements Listener {

    private final TeleportHistory history;

    TeleportHistoryListener(TeleportHistory history) {
        this.history = history;
    }

    /** Remembers the pre-teleport location so a later {@code /back} can return here. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom() != null) {
            history.remember(event.getPlayer().getUniqueId(), event.getFrom());
        }
    }

    /** Remembers the death location so {@code /back} returns to it. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        history.remember(player.getUniqueId(), player.getLocation());
    }
}
