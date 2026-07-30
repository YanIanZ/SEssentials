package dev.iyanz.sessentials.module.buildtools;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears the join/quit broadcast for players who toggled {@code /silence} on.
 *
 * <p>Runs at {@link EventPriority#HIGHEST} so it fires after the join-messages
 * module's {@link EventPriority#NORMAL} rewrite (and any other decorator) and the
 * per-player suppression always wins. Setting the event message is pure event data —
 * no entity/world mutation — and both events already fire on the involved player's
 * region thread, so no scheduler hop is needed (the same reasoning as
 * {@code module.joinmessages.JoinMessagesListener}).</p>
 */
final class SilenceListener implements Listener {

    private final SilenceFlags flags;

    /**
     * @param flags the persistent per-player silence flags
     */
    SilenceListener(SilenceFlags flags) {
        this.flags = flags;
    }

    /** Suppresses the join broadcast for a silenced player. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (flags.isSilenced(event.getPlayer().getUniqueId())) {
            event.joinMessage(null);
        }
    }

    /** Suppresses the quit broadcast for a silenced player. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (flags.isSilenced(event.getPlayer().getUniqueId())) {
            event.quitMessage(null);
        }
    }
}
