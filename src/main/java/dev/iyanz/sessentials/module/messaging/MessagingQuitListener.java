package dev.iyanz.sessentials.module.messaging;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Drops a disconnecting player's private-messaging state so the module's per-player
 * collections stay bounded.
 *
 * <p>The quitting player is removed from the social-spy set and from the reply-tracking
 * map both as a <em>key</em> (who they last messaged) and as a <em>value</em> (anyone
 * whose last correspondent was the quitting player, whose now-stale reply target would
 * otherwise linger). Without the value sweep the map would still grow without bound as
 * players who were messaged by, but never messaged back, someone who has since left.</p>
 *
 * <p>The event fires on the quitting player's own region thread; both collections are
 * concurrent, so the removals are thread-safe and need no scheduler hop.</p>
 */
final class MessagingQuitListener implements Listener {

    private final Map<UUID, UUID> lastMessaged;
    private final Set<UUID> spies;

    /**
     * @param lastMessaged shared reply-tracking map to clean up on quit
     * @param spies        shared social-spy set to clean up on quit
     */
    MessagingQuitListener(Map<UUID, UUID> lastMessaged, Set<UUID> spies) {
        this.lastMessaged = lastMessaged;
        this.spies = spies;
    }

    /** Removes the quitting player from the spy set and both directions of the reply map. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        spies.remove(id);
        lastMessaged.remove(id);
        lastMessaged.values().removeIf(id::equals);
    }
}
