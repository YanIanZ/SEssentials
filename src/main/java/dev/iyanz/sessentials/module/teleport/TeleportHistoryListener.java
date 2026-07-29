package dev.iyanz.sessentials.module.teleport;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Feeds {@link TeleportHistory} so {@code /back} works: remembers where a player
 * teleported <em>from</em> before every teleport, and where they died. Both events
 * fire on the affected player's own region thread, so reading their location here is
 * Folia-safe without any extra scheduler hop.
 *
 * <p>Also cleans up per-player teleport state on disconnect — the {@code /back}
 * history entry and any pending {@code /tpa}/{@code /tpahere} request — so neither map
 * grows without bound.</p>
 */
final class TeleportHistoryListener implements Listener {

    private final TeleportHistory history;
    private final TeleportRequests requests;

    TeleportHistoryListener(TeleportHistory history, TeleportRequests requests) {
        this.history = history;
        this.requests = requests;
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

    /** Drops the quitting player's {@code /back} history and any pending teleport request. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        history.remove(playerId);
        requests.removePlayer(playerId);
    }
}
