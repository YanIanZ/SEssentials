package dev.iyanz.sessentials.module.cmdwarmup;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Delays configured commands by a movement-cancellable warmup, per the durations held in
 * {@link CmdWarmupSettings}.
 *
 * <p>On a matching, non-bypassed command the {@link PlayerCommandPreprocessEvent} is
 * cancelled, the player's current location and full command line are recorded in a
 * {@link PendingWarmup}, and a delayed re-run is scheduled on the player's own region
 * thread via their {@link org.bukkit.entity.Entity#getScheduler() EntityScheduler}. If the
 * player is still within {@value #MAX_DRIFT_BLOCKS} blocks of where they started once the
 * delay elapses, the command is re-run for real via {@link Player#performCommand(String)};
 * otherwise the warmup is abandoned and the player is told why.</p>
 *
 * <p>A second command attempt while one is already pending replaces it outright — the
 * first scheduled re-run is cancelled and overwritten. Moving to a different block during
 * the warmup cancels it immediately via {@link #onMove}, rather than waiting for the full
 * delay to elapse.</p>
 *
 * <p>Re-running the command through {@link Player#performCommand(String)} fires this same
 * {@link PlayerCommandPreprocessEvent} again. A short-lived per-player "just fired" marker
 * lets that specific re-run pass straight through instead of being warmed up a second time
 * (which would otherwise loop forever).</p>
 *
 * <p>Folia-safe: {@link PlayerCommandPreprocessEvent} and {@link PlayerMoveEvent} both fire
 * on the affected player's own region thread, and the scheduled re-run is dispatched via
 * that same player's {@code EntityScheduler}, so no scheduler hop is needed anywhere in
 * this listener.</p>
 */
final class CmdWarmupListener implements Listener {

    private static final String BYPASS_PERMISSION = "sessentials.cmdwarmup.bypass";

    /** Max distance (in blocks) a player may drift from their start point and still complete a warmup. */
    private static final double MAX_DRIFT_BLOCKS = 0.5;

    private static final long TICKS_PER_SECOND = 20L;

    private final SEssentialsPlugin plugin;
    private final CmdWarmupSettings settings;

    /** Pending warmups, keyed by player UUID. A new attempt replaces (and cancels) the old one. */
    private final Map<UUID, PendingWarmup> pending = new ConcurrentHashMap<>();

    /** Players whose very next {@link PlayerCommandPreprocessEvent} should bypass warmup once. */
    private final Set<UUID> justFired = ConcurrentHashMap.newKeySet();

    /**
     * @param plugin   the owning plugin, used to schedule the delayed re-run
     * @param settings the shared, reloadable warmup durations to enforce
     */
    CmdWarmupListener(SEssentialsPlugin plugin, CmdWarmupSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** Cancels a matching command, starts its warmup, and schedules the delayed re-run. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Let a warmup's own re-run pass straight through, exactly once, to avoid re-warming it.
        if (justFired.remove(uuid)) {
            return;
        }
        if (!settings.enabled()) {
            return;
        }

        String label = label(event.getMessage());
        Long seconds = settings.seconds(label);
        if (seconds == null) {
            return;
        }
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        event.setCancelled(true);

        String rawMessage = event.getMessage();
        String withoutSlash = rawMessage.startsWith("/") ? rawMessage.substring(1) : rawMessage;

        PendingWarmup previous = pending.remove(uuid);
        if (previous != null) {
            previous.cancelTask();
        }

        PendingWarmup warmup = new PendingWarmup(player.getLocation(), withoutSlash);
        pending.put(uuid, warmup);
        Msg.info(player, "Don't move for " + seconds + " seconds...");

        warmup.task(player.getScheduler().runDelayed(plugin,
                scheduledTask -> fireWarmup(player, uuid, warmup), null, seconds * TICKS_PER_SECOND));
    }

    /** Cancels a player's pending warmup the instant they step into a different block. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        PendingWarmup removed = pending.remove(player.getUniqueId());
        if (removed != null) {
            removed.cancelTask();
            Msg.err(player, "Warmup cancelled — you moved.");
        }
    }

    /** Drops a disconnecting player's pending warmup (and cancels its scheduled re-run). */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        justFired.remove(uuid);
        PendingWarmup removed = pending.remove(uuid);
        if (removed != null) {
            removed.cancelTask();
        }
    }

    /**
     * Runs on {@code player}'s region thread once their warmup delay elapses: re-runs the
     * command if they haven't drifted too far, otherwise reports the cancellation.
     *
     * @param player   the warming-up player
     * @param uuid     the player's UUID (used to key the pending/just-fired tracking maps)
     * @param expected the warmup that was scheduled; only acted on if it is still the
     *                 player's current pending warmup (not replaced or cancelled already)
     */
    private void fireWarmup(Player player, UUID uuid, PendingWarmup expected) {
        if (!pending.remove(uuid, expected)) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        if (hasDrifted(player, expected.startLocation())) {
            Msg.err(player, "Warmup cancelled — you moved.");
            return;
        }

        Msg.ok(player, "Warming up complete.");
        justFired.add(uuid);
        try {
            player.performCommand(expected.fullCommand());
        } finally {
            justFired.remove(uuid);
        }
    }

    /** @return whether {@code player} is now more than {@link #MAX_DRIFT_BLOCKS} from {@code start}. */
    private static boolean hasDrifted(Player player, Location start) {
        Location now = player.getLocation();
        if (start.getWorld() == null || now.getWorld() == null || !now.getWorld().equals(start.getWorld())) {
            return true;
        }
        return now.distance(start) > MAX_DRIFT_BLOCKS;
    }

    /** @return the lowercase command label (no leading slash), parsed from the raw preprocess message. */
    private static String label(String rawMessage) {
        String firstToken = rawMessage.split(" ", 2)[0];
        String withoutSlash = firstToken.startsWith("/") ? firstToken.substring(1) : firstToken;
        return withoutSlash.toLowerCase(Locale.ROOT);
    }
}
