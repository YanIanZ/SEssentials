package dev.iyanz.sessentials.module.timedcommands;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Runs configured console commands on fixed, per-task intervals — e.g. periodic
 * auto-save reminders or maintenance commands.
 *
 * <p>Reads {@code timed-commands.enabled} (default {@code true}) and the
 * {@code timed-commands.tasks} map (see {@link TimedCommandsSettings}) once at
 * {@link #enable}. Each configured task gets its own repeating async task
 * ({@link Schedulers#asyncRepeating}), scheduled at {@code interval-seconds * 20}
 * ticks; every firing dispatches that task's commands, in order, as the console.
 * If {@code tasks} is empty (or the feature is disabled), nothing is scheduled.</p>
 *
 * <p>{@code /timedcommands reload} re-reads the config, cancels every running task and
 * reschedules from the fresh settings (so edits to intervals, commands, task ids or the
 * top-level {@code enabled} flag take effect immediately). {@code /timedcommands list}
 * reports every configured task id and its interval.</p>
 *
 * <p>Folia-safe: no task ever touches entity/world state. Each firing hops to the
 * global region scheduler ({@link Bukkit#getGlobalRegionScheduler()}) to dispatch its
 * commands as the console, exactly like a player typing them would be routed.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class TimedCommandsModule implements EssModule {

    /** Ticks per second, used to convert a configured interval to Bukkit ticks. */
    private static final long TICKS_PER_SECOND = 20L;

    private SEssentialsPlugin plugin;
    private TimedCommandsSettings settings;
    private final List<ScheduledTask> tasks = new ArrayList<>();

    @Override
    public String name() {
        return "timedcommands";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.settings = TimedCommandsSettings.load(plugin.getConfig());

        plugin.commands(reg -> reg.register(Commands.literal("timedcommands")
                .requires(s -> s.getSender().hasPermission("sessentials.timedcommands"))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            plugin.reloadConfig();
                            settings.reload(plugin.getConfig());
                            scheduleAll();
                            Msg.ok(ctx.getSource().getSender(), "Timed-commands config reloaded.");
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            list(ctx.getSource().getSender());
                            return 1;
                        }))
                .build(), "Manage scheduled console-command tasks"));

        scheduleAll();
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        cancelAll();
    }

    /** Reports every configured task's id and interval, or that none are set. */
    private void list(CommandSender sender) {
        List<TimedTaskDefinition> configured = settings.tasks();
        if (configured.isEmpty()) {
            Msg.info(sender, "There are no timed-command tasks configured.");
            return;
        }
        Msg.info(sender, "Timed-command tasks:");
        for (TimedTaskDefinition task : configured) {
            Msg.value(sender, task.id() + ":", task.intervalSeconds() + "s");
        }
    }

    /**
     * Cancels any running tasks, then — if the feature is enabled — starts one fresh
     * repeating task per configured entry at its own interval.
     */
    private synchronized void scheduleAll() {
        cancelAll();
        if (!settings.enabled()) {
            return;
        }
        for (TimedTaskDefinition task : settings.tasks()) {
            long periodTicks = Math.max(1L, task.intervalSeconds() * TICKS_PER_SECOND);
            tasks.add(Schedulers.asyncRepeating(plugin, () -> dispatch(task.commands()), periodTicks, periodTicks));
        }
    }

    /** Cancels and forgets every currently scheduled task. */
    private synchronized void cancelAll() {
        for (ScheduledTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }

    /** Dispatches each of {@code commands}, in order, as the console on the global region thread. */
    private void dispatch(List<String> commands) {
        for (String command : commands) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }
}
