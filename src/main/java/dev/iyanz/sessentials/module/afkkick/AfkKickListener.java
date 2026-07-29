package dev.iyanz.sessentials.module.afkkick;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Feeds the {@link AfkActivityTracker} from player activity: movement (only when the
 * player crosses into a new block, to avoid per-tick look spam), chat, commands,
 * interactions, and joins. A quit always drops the player's entry so the map never
 * leaks across sessions.
 *
 * <p>Activity handlers do nothing while the feature is disabled, keeping the map empty
 * until {@code afk-kick.enabled} is turned on. Every handler body is deliberately
 * cheap — a single {@link AfkActivityTracker#touch} map write — because
 * {@link #onMove} in particular fires extremely frequently.</p>
 */
public final class AfkKickListener implements Listener {

    private final AfkKickSettings settings;
    private final AfkActivityTracker tracker;

    /**
     * @param settings the reloadable module settings (read for the enabled flag)
     * @param tracker  the shared activity tracker to stamp
     */
    public AfkKickListener(AfkKickSettings settings, AfkActivityTracker tracker) {
        this.settings = settings;
        this.tracker = tracker;
    }

    /** Records activity when a player moves into a new block (ignores look-only moves). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !settings.enabled()) {
            return;
        }
        tracker.touch(event.getPlayer().getUniqueId());
    }

    /** Records activity when a player sends a chat message. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        markActive(event.getPlayer());
    }

    /** Records activity when a player runs a command. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        markActive(event.getPlayer());
    }

    /** Records activity when a player interacts (clicks a block, air, or item). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        markActive(event.getPlayer());
    }

    /** Seeds a fresh activity entry when a player joins. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        markActive(event.getPlayer());
    }

    /** Drops the player's tracking entry when they disconnect. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.remove(event.getPlayer().getUniqueId());
    }

    /** Stamps {@code player} as active, but only while the feature is enabled. */
    private void markActive(Player player) {
        if (!settings.enabled()) {
            return;
        }
        tracker.touch(player.getUniqueId());
    }
}
