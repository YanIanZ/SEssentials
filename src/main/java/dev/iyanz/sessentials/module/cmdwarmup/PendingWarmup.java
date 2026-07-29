package dev.iyanz.sessentials.module.cmdwarmup;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;

/**
 * A player's in-flight command warmup: the location they were standing at when it began,
 * the full command line (no leading slash) to re-run once the warmup elapses, and a
 * handle to the scheduled re-run so it can be cancelled early.
 *
 * <p>A warmup can be cancelled early for three reasons: a second command attempt replaces
 * it, the player moves to a different block, or the player disconnects. In every case the
 * owning {@link CmdWarmupListener} removes this instance from its pending map and calls
 * {@link #cancelTask()}.</p>
 */
final class PendingWarmup {

    private final Location startLocation;
    private final String fullCommand;
    private volatile ScheduledTask task;

    /**
     * @param startLocation the player's location when the warmup began
     * @param fullCommand   the full command line (no leading slash) to re-run on completion
     */
    PendingWarmup(Location startLocation, String fullCommand) {
        this.startLocation = startLocation;
        this.fullCommand = fullCommand;
    }

    /** @return the location the player was standing at when the warmup started. */
    Location startLocation() {
        return startLocation;
    }

    /** @return the full command line (no leading slash) to re-run once the warmup elapses. */
    String fullCommand() {
        return fullCommand;
    }

    /** Records the scheduled re-run task so it can later be cancelled via {@link #cancelTask()}. */
    void task(ScheduledTask task) {
        this.task = task;
    }

    /** Cancels the scheduled re-run, if it hasn't fired yet. Safe to call more than once. */
    void cancelTask() {
        ScheduledTask t = task;
        if (t != null) {
            t.cancel();
        }
    }
}
