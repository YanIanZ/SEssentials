package dev.iyanz.sessentials.module.playerstate;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears a disconnecting player's god-mode flag so the {@link GodService} registry does
 * not retain entries for players who have left.
 *
 * <p>The event fires on the quitting player's own region thread; the work is a single
 * removal from a concurrent set, so no scheduler hop is required.</p>
 *
 * @see GodService
 */
final class GodQuitListener implements Listener {

    private final GodService godService;

    /**
     * @param godService the shared god-mode registry to clean up on quit
     */
    GodQuitListener(GodService godService) {
        this.godService = godService;
    }

    /** Removes the quitting player from the god-mode registry. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        godService.remove(event.getPlayer().getUniqueId());
    }
}
