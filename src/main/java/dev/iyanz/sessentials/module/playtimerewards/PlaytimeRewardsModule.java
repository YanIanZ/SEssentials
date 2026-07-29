package dev.iyanz.sessentials.module.playtimerewards;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Grants configured one-off rewards (console commands and/or a message) the first
 * time an online player's total playtime crosses a configured milestone.
 *
 * <p>Reads {@code playtime-rewards.enabled} (default {@code true}),
 * {@code playtime-rewards.check-interval-seconds} (default {@code 60}) and the
 * {@code playtime-rewards.rewards} map (see {@link PlaytimeRewardsSettings}) once at
 * {@link #enable}; if no reward is configured, one demonstrative, message-only
 * 60-minute reward is seeded into {@code config.yml} so the feature is immediately
 * visible. A single repeating async task ({@link Schedulers#asyncRepeating}) then
 * walks every online player once per interval, hopping to each player's own region
 * thread to read their {@link org.bukkit.Statistic#PLAY_ONE_MINUTE} statistic and
 * grant anything newly unlocked (see {@link PlaytimeRewardsService}).</p>
 *
 * <p>{@code /playtimerewards check} lets a player force an immediate check of their
 * own playtime (bypassing the {@code enabled} flag, since it is an explicit manual
 * action), and {@code /playtimerewards reload} re-reads the config, atomically
 * swapping in the new reward list/interval/enabled flag and rescheduling the
 * background task to the new interval.</p>
 *
 * <p>Folia-safe: the background task itself never touches entity/world state
 * directly; all statistic reads, reward grants and messages happen after hopping to
 * the target player's region thread via {@link Schedulers#entity}, and reward
 * commands are dispatched as console on Folia's global region thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class PlaytimeRewardsModule implements EssModule {

    /** Ticks per second, used to convert the configured interval to Bukkit ticks. */
    private static final long TICKS_PER_SECOND = 20L;

    private SEssentialsPlugin plugin;
    private PlaytimeRewardsSettings settings;
    private ScheduledTask task;

    @Override
    public String name() {
        return "playtimerewards";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        PlaytimeRewardsSettings.seedDefaultIfEmpty(plugin);
        this.settings = PlaytimeRewardsSettings.load(plugin.getConfig());

        plugin.commands(reg -> reg.register(Commands.literal("playtimerewards")
                .then(Commands.literal("check")
                        .requires(s -> s.getSender().hasPermission("sessentials.playtimerewards"))
                        .executes(Cmds.playerExec(this::forceCheck)))
                .then(Commands.literal("reload")
                        .requires(s -> s.getSender().hasPermission("sessentials.playtimerewards.admin"))
                        .executes(ctx -> {
                            plugin.reloadConfig();
                            settings.reload(plugin.getConfig());
                            rescheduleTask();
                            Msg.ok(ctx.getSource().getSender(), "Playtime-rewards config reloaded.");
                            return 1;
                        }))
                .build(), "Playtime reward milestones"));

        rescheduleTask();
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        cancelTask();
    }

    /** Forces an immediate reward check for the invoking player, regardless of {@code enabled}. */
    private void forceCheck(Player player) {
        PlaytimeRewardsService.check(plugin, settings, player);
        Msg.ok(player, "Checked your playtime rewards.");
    }

    /** Runs one pass over every online player, skipped entirely while the feature is disabled. */
    private void runCheck() {
        if (!settings.enabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> PlaytimeRewardsService.check(plugin, settings, player));
        }
    }

    /** Cancels any running background task and starts a new one at the current interval. */
    private synchronized void rescheduleTask() {
        cancelTask();
        long periodTicks = Math.max(1L, settings.checkIntervalSeconds() * TICKS_PER_SECOND);
        this.task = Schedulers.asyncRepeating(plugin, this::runCheck, periodTicks, periodTicks);
    }

    /** Cancels the background task, if one is running. */
    private synchronized void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
