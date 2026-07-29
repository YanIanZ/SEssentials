package dev.iyanz.sessentials.module.tempfly;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Tracks and applies temporary-flight grants. Each grant enables flight for a player,
 * schedules an auto-revoke on that player's entity scheduler, and keeps a handle to the
 * revoke task keyed by player UUID so a re-grant can cancel the previous timer (durations
 * never stack) and so a disconnect can clean up.
 *
 * <p>The map is a {@link ConcurrentHashMap} because entries are touched from several
 * region threads: a grant for player A runs on A's region thread, while a quit for player
 * B runs on B's thread, and both may hit this shared map at once.</p>
 *
 * <p>Folia-safe: every flight mutation runs on the affected player's region thread —
 * {@link #grant} hops there via {@link Schedulers#entity}, and the auto-revoke runs on the
 * player's own {@link Player#getScheduler() entity scheduler}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class TempFlyGrants {

    /** Ticks per second, used to convert a seconds duration into the scheduler's tick delay. */
    private static final long TICKS_PER_SECOND = 20L;

    /** Active revoke tasks, keyed by the player they belong to. */
    private final ConcurrentHashMap<UUID, ScheduledTask> active = new ConcurrentHashMap<>();

    /**
     * Grants {@code target} temporary flight for {@code seconds} seconds. The flight
     * mutation and the auto-revoke scheduling are performed on the target's region thread.
     *
     * @param plugin  the owning plugin, used to schedule on the target's region thread
     * @param sender  the command sender (messaged when acting on someone else)
     * @param target  the player receiving temporary flight
     * @param seconds the duration in seconds (assumed already range-checked by the caller)
     */
    void grant(SEssentialsPlugin plugin, CommandSender sender, Player target, int seconds) {
        Schedulers.entity(plugin, target, () -> apply(plugin, sender, target, seconds));
    }

    /**
     * Applies the grant on the target's region thread: cancels any previous timer so
     * durations don't stack, enables flight, notifies the involved parties, and schedules
     * the auto-revoke.
     */
    private void apply(SEssentialsPlugin plugin, CommandSender sender, Player target, int seconds) {
        UUID id = target.getUniqueId();

        ScheduledTask previous = active.remove(id);
        if (previous != null) {
            previous.cancel();
        }

        target.setAllowFlight(true);
        Msg.ok(target, "Temporary flight granted for " + seconds + " seconds.");
        if (sender != target) {
            Msg.ok(sender, "Granted " + seconds + " seconds of flight to " + target.getName() + ".");
        }

        ScheduledTask[] self = new ScheduledTask[1];
        self[0] = target.getScheduler().runDelayed(plugin, task -> {
            target.setAllowFlight(false);
            target.setFlying(false);
            Msg.info(target, "Temporary flight expired.");
            active.remove(id, self[0]);
        }, null, seconds * TICKS_PER_SECOND);

        if (self[0] != null) {
            active.put(id, self[0]);
        }
    }

    /**
     * Cancels and forgets a player's pending revoke task, if any. Called when a player
     * quits — their flight state leaves with them, so only the timer needs clearing.
     *
     * @param id the player's UUID
     */
    void revoke(UUID id) {
        ScheduledTask task = active.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    /** Cancels every outstanding revoke task and clears the map (module shutdown). */
    void cancelAll() {
        for (ScheduledTask task : active.values()) {
            task.cancel();
        }
        active.clear();
    }
}
