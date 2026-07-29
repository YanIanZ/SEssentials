package dev.iyanz.sessentials.module.slowmode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Enforces the global chat slowmode: while {@link SlowModeState#seconds()} is greater than
 * zero, every non-exempt player must wait that many seconds between chat messages. A
 * message sent too soon is cancelled and the sender is told how long is left; an accepted
 * message stamps the player's clock so the next gap is measured from it.
 *
 * <p>Players holding {@code sessentials.slowmode.bypass} are never limited, and a slowmode
 * of {@code 0} short-circuits the check entirely so chat is unaffected when the feature is
 * off.</p>
 *
 * <p>Runs at {@link EventPriority#LOW} with {@code ignoreCancelled = true} so it sits after
 * lower-priority gates (such as a mute check): an already-cancelled message is neither
 * counted nor nagged about. Folia-safe: chat events already fire off-region, the tracking
 * map is a {@link ConcurrentHashMap}, and the only side effects are cancelling the event
 * and messaging the sender — both thread-safe.</p>
 */
public final class SlowModeListener implements Listener {

    /** Permission that exempts a player from the global chat slowmode. */
    private static final String BYPASS = "sessentials.slowmode.bypass";

    /** Milliseconds in one second, used to convert the configured gap. */
    private static final long MILLIS_PER_SECOND = 1000L;

    /** Shared, live view of the current slowmode gap in seconds. */
    private final SlowModeState state;

    /** Epoch-millis of each player's last accepted message. */
    private final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();

    /**
     * @param state the shared holder the module's command writes and this listener reads
     */
    public SlowModeListener(SlowModeState state) {
        this.state = state;
    }

    /** Applies the global slowmode gap to an outgoing chat message. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        int seconds = state.seconds();
        if (seconds <= 0) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS)) {
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long gapMillis = seconds * MILLIS_PER_SECOND;

        Long last = lastMessage.get(id);
        if (last != null && now - last < gapMillis) {
            event.setCancelled(true);
            long remainingSeconds = (gapMillis - (now - last) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND;
            Msg.err(player, "Slowmode: wait " + remainingSeconds + "s.");
            return;
        }
        lastMessage.put(id, now);
    }

    /** Drops a player's tracked timestamp on disconnect, keeping the map bounded. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastMessage.remove(event.getPlayer().getUniqueId());
    }
}
