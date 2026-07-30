package dev.iyanz.sessentials.module.identity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
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
 * CMI-style automatic AFK on idle, layered on top of the identity module's manual
 * {@code /afk} ({@link AfkCommand}) and shared {@link AfkService}.
 *
 * <p>After {@code afk.auto-seconds} of no activity a player is <em>automatically</em>
 * marked AFK — the exact same state change and "is now AFK" broadcast the manual command
 * uses — and on their next tracked activity (moving to a new block, chatting, running a
 * command, or interacting) they are automatically unmarked again ("no longer AFK"). This
 * class only ever marks/clears AFK; it never kicks — idle kicking is a separate concern
 * owned by the {@code afkkick} module.</p>
 *
 * <p>Design:</p>
 * <ul>
 *   <li>A per-session {@link ConcurrentHashMap} records each online player's last-activity
 *       time (epoch millis). It is written from the activity listeners (any region thread,
 *       or async for chat) and read from the async sweep, hence the concurrent map.</li>
 *   <li>A single repeating async task ({@link Schedulers#asyncRepeating}, every
 *       {@value #SWEEP_PERIOD_TICKS} ticks) sweeps every online player; anyone idle past
 *       the window who is not already AFK is flipped on their own region thread.</li>
 *   <li><b>Double-mark guard:</b> the sweep skips players already AFK, and the actual mark
 *       is {@link AfkService#markAuto} ({@code putIfAbsent}) which only broadcasts when it
 *       genuinely transitions a not-AFK player to AFK.</li>
 *   <li><b>Manual vs auto:</b> activity only auto-clears <em>auto</em> marks via
 *       {@link AfkService#clearIfAuto}; a manual {@code /afk} is never cleared by moving,
 *       matching CMI.</li>
 * </ul>
 *
 * <p>Folia-safe: the sweep touches no entity/world state on the async thread; the mark and
 * its broadcast are dispatched onto the target player's region thread via
 * {@link Schedulers#entity}. Set {@code afk.auto-seconds} to {@code 0} to disable the
 * feature entirely — the listener does nothing and no sweep is scheduled.</p>
 */
public final class AutoAfk implements Listener {

    /** Config key for the idle window, in seconds ({@code 0} disables auto-AFK). */
    static final String CONFIG_KEY = "afk.auto-seconds";
    /** Default idle window before an auto-AFK, in seconds (5 minutes). */
    private static final int DEFAULT_SECONDS = 300;
    /** Sweep cadence in ticks (100 ticks = 5 seconds at 20 TPS). */
    private static final long SWEEP_PERIOD_TICKS = 100L;
    /** Milliseconds in one second, used to convert the configured idle window. */
    private static final long MILLIS_PER_SECOND = 1000L;

    private final SEssentialsPlugin plugin;
    private final AfkService afkService;

    /** UUID -&gt; last-activity epoch millis; per-session, dropped on quit. */
    private final ConcurrentHashMap<UUID, Long> lastActive = new ConcurrentHashMap<>();

    /** {@code true} when {@code afk.auto-seconds > 0}. */
    private final boolean enabled;
    /** Idle window in millis before an auto-AFK; only meaningful when {@link #enabled}. */
    private final long idleMillis;

    private ScheduledTask task;

    /**
     * @param plugin     the owning plugin (config, scheduler, listener registration)
     * @param afkService the shared AFK-state tracker (manual and auto marks live here)
     */
    public AutoAfk(SEssentialsPlugin plugin, AfkService afkService) {
        this.plugin = plugin;
        this.afkService = afkService;
        int seconds = plugin.getConfig().getInt(CONFIG_KEY, DEFAULT_SECONDS);
        this.enabled = seconds > 0;
        this.idleMillis = (long) seconds * MILLIS_PER_SECOND;
    }

    /**
     * Registers the activity listener and schedules the idle sweep. A no-op when the
     * feature is disabled ({@code afk.auto-seconds <= 0}), so nothing is tracked and no
     * async task runs.
     */
    public void start() {
        if (!enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.task = Schedulers.asyncRepeating(plugin, this::sweep, SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
    }

    /** Cancels the sweep and drops all tracking, e.g. on plugin disable. */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastActive.clear();
    }

    // --- activity feed (mirrors the events afkkick's tracker listens to) ---

    /** Records activity when a player moves into a new block (ignores look-only moves). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock()) {
            onActivity(event.getPlayer());
        }
    }

    /** Records activity when a player sends a chat message. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        onActivity(event.getPlayer());
    }

    /** Records activity when a player runs a command. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        onActivity(event.getPlayer());
    }

    /** Records activity when a player interacts (clicks a block, air, or item). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        onActivity(event.getPlayer());
    }

    /** Seeds a fresh activity entry when a player joins. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lastActive.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    /** Drops the player's tracking entry when they disconnect. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastActive.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Stamps a player's last-activity time and, if they were <em>auto</em>-AFK, clears it
     * and broadcasts "no longer AFK". A manual {@code /afk} is deliberately left in place.
     * Runs on whatever thread the source event fired on (region, or async for chat); the
     * state mutation is atomic and messaging is thread-safe.
     */
    private void onActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastActive.put(uuid, System.currentTimeMillis());
        if (afkService.clearIfAuto(uuid)) {
            AfkCommand.announce(player, false, null);
        }
    }

    /**
     * One sweep over every online player: auto-marks anyone idle past the window who is
     * not already AFK. Players without a tracked timestamp are seeded (so enabling the
     * feature never AFKs someone on the very first sweep). The mark itself is dispatched to
     * the player's region thread, where {@link AfkService#markAuto} atomically guards
     * against a double-mark before broadcasting.
     */
    private void sweep() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (afkService.isAfk(uuid)) {
                continue;
            }
            Long last = lastActive.get(uuid);
            if (last == null) {
                lastActive.put(uuid, now);
                continue;
            }
            if (now - last > idleMillis) {
                Schedulers.entity(plugin, player, () -> {
                    if (afkService.markAuto(uuid)) {
                        AfkCommand.announce(player, true, null);
                    }
                });
            }
        }
    }
}
