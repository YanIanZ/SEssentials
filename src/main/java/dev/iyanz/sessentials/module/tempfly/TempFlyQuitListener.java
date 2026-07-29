package dev.iyanz.sessentials.module.tempfly;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears a player's pending temporary-flight revoke when they disconnect, so a stale task
 * never fires against an offline player and the {@link TempFlyGrants} map does not retain
 * entries for players who have left.
 *
 * <p>The event fires on the quitting player's own region thread; the work here is a single
 * map removal plus a task cancel (both handled by {@link TempFlyGrants}), so no scheduler
 * hop is required.</p>
 */
final class TempFlyQuitListener implements Listener {

    private final TempFlyGrants grants;

    /**
     * @param grants the shared grant registry to clean up on quit
     */
    TempFlyQuitListener(TempFlyGrants grants) {
        this.grants = grants;
    }

    /** Cancels and removes the quitting player's pending flight-revoke task, if any. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        grants.revoke(event.getPlayer().getUniqueId());
    }
}
