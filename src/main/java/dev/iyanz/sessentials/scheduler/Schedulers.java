package dev.iyanz.sessentials.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Thin, Folia-safe helpers over Paper's threaded-region schedulers
 * ({@code io.papermc.paper.threadedregions.scheduler}).
 *
 * <p>On Folia the legacy {@code Bukkit.getScheduler()} main-thread scheduler does
 * not exist. All plugin work must be routed to the correct scheduler:</p>
 * <ul>
 *   <li><b>Entity work</b> (open inventory, send message, teleport) → the owning
 *       entity's {@link Entity#getScheduler() EntityScheduler}.</li>
 *   <li><b>DB / heavy compute</b> → the global {@link AsyncScheduler}.</li>
 * </ul>
 *
 * <p>This class is stateless; every method delegates to the server schedulers.</p>
 */
public final class Schedulers {

    /** Milliseconds in a single Minecraft tick (20 TPS). */
    private static final long MILLIS_PER_TICK = 50L;

    private Schedulers() {
    }

    /**
     * Runs {@code task} on the scheduler that owns {@code entity}'s region, on the
     * next tick. Safe for entity-affecting API calls.
     *
     * @param plugin the owning plugin
     * @param entity the entity whose scheduler executes the task (e.g. a {@code Player})
     * @param task   the work to run
     * @return the scheduled task, or {@code null} if the entity was retired/removed
     */
    public static ScheduledTask entity(Plugin plugin, Entity entity, Runnable task) {
        return entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    /**
     * Runs {@code task} once on the async scheduler as soon as possible. Never touch
     * entity/world API from here — use {@link #entity(Plugin, Entity, Runnable)} to hop back.
     *
     * @param plugin the owning plugin
     * @param task   the work to run; receives the {@link ScheduledTask} handle
     * @return the scheduled task handle
     */
    public static ScheduledTask async(Plugin plugin, Consumer<ScheduledTask> task) {
        return Bukkit.getAsyncScheduler().runNow(plugin, task);
    }

    /**
     * Schedules a repeating async task. Tick arguments are converted to the
     * millisecond units the {@link AsyncScheduler} expects (1 tick = 50 ms), and
     * are clamped to a minimum of 1 ms because the scheduler rejects non-positive
     * delays/periods.
     *
     * @param plugin      the owning plugin
     * @param task        the work to run each period
     * @param delayTicks  initial delay in ticks before the first run
     * @param periodTicks period in ticks between subsequent runs
     * @return the scheduled task handle
     */
    public static ScheduledTask asyncRepeating(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        final AsyncScheduler scheduler = Bukkit.getAsyncScheduler();
        final long delayMillis = Math.max(1L, delayTicks * MILLIS_PER_TICK);
        final long periodMillis = Math.max(1L, periodTicks * MILLIS_PER_TICK);
        return scheduler.runAtFixedRate(plugin, scheduled -> task.run(), delayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }
}
