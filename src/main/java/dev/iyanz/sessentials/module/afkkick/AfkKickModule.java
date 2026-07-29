package dev.iyanz.sessentials.module.afkkick;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Automatically kicks players who have been idle for too long.
 *
 * <p>Reads the {@code afk-kick} config section (see {@link AfkKickSettings};
 * {@code enabled} defaults to {@code false}, {@code seconds} to {@code 600}) once at
 * {@link #enable}. An {@link AfkKickListener} stamps each player's last-activity time
 * from movement, chat, commands, interactions and joins into a shared
 * {@link AfkActivityTracker}, and a single repeating async task
 * ({@link Schedulers#asyncRepeating}, every 100 ticks / 5s) sweeps every online player:
 * anyone whose idle time exceeds the configured window — and who lacks the bypass
 * permission — is kicked on their own region thread and dropped from tracking.</p>
 *
 * <p>{@code /afkkick reload} re-reads the config and re-applies the settings in place;
 * because the sweep task early-returns while disabled, toggling {@code enabled} via
 * reload takes effect on the next sweep without rescheduling.</p>
 *
 * <p>Folia-safe: the sweep task never mutates entity/world state directly; the kick is
 * dispatched via {@link Schedulers#entity} onto the target player's region thread.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class AfkKickModule implements EssModule {

    /** Sweep cadence in ticks (100 ticks = 5 seconds at 20 TPS). */
    private static final long SWEEP_PERIOD_TICKS = 100L;

    private SEssentialsPlugin plugin;
    private AfkKickSettings settings;
    private AfkActivityTracker tracker;
    private ScheduledTask task;

    @Override
    public String name() {
        return "afkkick";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        this.settings = AfkKickSettings.load(plugin.getConfig());
        this.tracker = new AfkActivityTracker();

        plugin.getServer().getPluginManager().registerEvents(new AfkKickListener(settings, tracker), plugin);

        plugin.commands(reg -> reg.register(Commands.literal("afkkick")
                .requires(s -> s.getSender().hasPermission("sessentials.afkkick"))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            plugin.reloadConfig();
                            settings.reload(plugin.getConfig());
                            Msg.ok(ctx.getSource().getSender(), "AFK-kick config reloaded.");
                            return 1;
                        }))
                .build(), "Auto-kick idle players"));

        this.task = Schedulers.asyncRepeating(plugin, this::sweep, SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (tracker != null) {
            tracker.clear();
        }
    }

    /**
     * One sweep over every online player: kicks anyone who has been idle past the
     * configured window. No-op while the feature is disabled. Players with the bypass
     * permission are skipped, and any player without a tracked timestamp is seeded with
     * one (so enabling the feature never kicks someone on the very first sweep).
     */
    private void sweep() {
        if (!settings.enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        long idleMillis = settings.idleMillis();
        String bypass = settings.bypassPermission();
        Component kickMessage = Component.text(settings.message());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(bypass)) {
                continue;
            }
            UUID uuid = player.getUniqueId();
            Long last = tracker.lastActive(uuid);
            if (last == null) {
                tracker.touch(uuid);
                continue;
            }
            if (now - last > idleMillis) {
                tracker.remove(uuid);
                Schedulers.entity(plugin, player, () -> player.kick(kickMessage));
            }
        }
    }
}
