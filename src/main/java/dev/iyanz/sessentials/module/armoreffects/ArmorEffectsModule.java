package dev.iyanz.sessentials.module.armoreffects;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Grants configured potion effects to players while they wear a matching piece of
 * armor (see {@link ArmorEffectsSettings} for the {@code armor-effects} config shape).
 *
 * <p>Reads {@code armor-effects.enabled} (default {@code true}) and the
 * {@code armor-effects.effects} rule list once at {@link #enable}; if none is
 * configured, two demonstrative rules (netherite chestplate → fire resistance, golden
 * boots → speed II) are seeded into {@code config.yml} so the feature is immediately
 * visible. A single repeating async task ({@link Schedulers#asyncRepeating}, every 40
 * ticks) then walks every online player, hopping to each player's own region thread to
 * read their worn armor and (re)apply any matching effect for a short 3-second window
 * (see {@link ArmorEffectsService}) — long enough to bridge one scheduler cycle, short
 * enough that the effect fades out soon after the armor comes off.</p>
 *
 * <p>{@code /armoreffects reload} re-reads the config, atomically swapping in the new
 * enabled flag/rule list and rescheduling the background task.</p>
 *
 * <p>Folia-safe: the background task itself never touches entity/world state directly;
 * every armor read and effect grant happens after hopping to the target player's region
 * thread via {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class ArmorEffectsModule implements EssModule {

    /** Ticks between background sweeps of every online player. */
    private static final long PERIOD_TICKS = 40L;

    private SEssentialsPlugin plugin;
    private ArmorEffectsSettings settings;
    private ScheduledTask task;

    @Override
    public String name() {
        return "armoreffects";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        this.plugin = plugin;
        ArmorEffectsSettings.seedDefaultIfEmpty(plugin);
        this.settings = ArmorEffectsSettings.load(plugin.getConfig());

        plugin.commands(reg -> reg.register(Commands.literal("armoreffects")
                .then(Commands.literal("reload")
                        .requires(s -> s.getSender().hasPermission("sessentials.armoreffects"))
                        .executes(ctx -> {
                            plugin.reloadConfig();
                            settings.reload(plugin.getConfig());
                            rescheduleTask();
                            Msg.ok(ctx.getSource().getSender(), "Armor-effects config reloaded.");
                            return 1;
                        }))
                .build(), "Armor-granted potion effects"));

        rescheduleTask();
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        cancelTask();
    }

    /** Runs one pass over every online player. */
    private void sweep() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, player, () -> ArmorEffectsService.apply(player, settings.rules()));
        }
    }

    /**
     * Cancels any running background task, then — if the feature is enabled — starts a
     * fresh one at the current period. A no-op task is left absent while disabled, so
     * toggling {@code armor-effects.enabled} off via {@code /armoreffects reload} fully
     * stops the background sweep rather than merely skipping its work.
     */
    private synchronized void rescheduleTask() {
        cancelTask();
        if (!settings.enabled()) {
            return;
        }
        this.task = Schedulers.asyncRepeating(plugin, this::sweep, PERIOD_TICKS, PERIOD_TICKS);
    }

    /** Cancels the background task, if one is running. */
    private synchronized void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
