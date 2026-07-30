package dev.iyanz.sessentials.module.cmiextras;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans the {@link TpaAllInvites} registry when a player disconnects, dropping both
 * the invite addressed to them and any invites they sent, so the in-memory map never
 * accumulates entries for players who logged off.
 */
final class TpaAllQuitListener implements Listener {

    private final TpaAllInvites invites;

    /**
     * @param invites the shared pending-invite registry to clean
     */
    TpaAllQuitListener(TpaAllInvites invites) {
        this.invites = invites;
    }

    /**
     * Drops all invite entries involving the quitting player.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invites.removePlayer(event.getPlayer().getUniqueId());
    }
}
