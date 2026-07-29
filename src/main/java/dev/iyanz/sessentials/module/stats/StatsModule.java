package dev.iyanz.sessentials.module.stats;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Reports a curated set of built-in {@link org.bukkit.Statistic} values — deaths,
 * player/mob kills, damage dealt/taken, jumps, total play time and distance
 * travelled — for the sender or another online player.
 *
 * <p>Command: {@code /stats [player]}. Every figure is read from Bukkit/Paper's own,
 * already-tracked statistics rather than custom bookkeeping, so this module needs no
 * persistence of its own. See {@link StatsCommand} for the command wiring (including
 * the Folia-safe region-thread hop for another player's statistics) and
 * {@link StatsFormat} for the human-readable unit conversions.</p>
 */
public final class StatsModule implements EssModule {

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        StatsCommand.register(plugin);
    }
}
