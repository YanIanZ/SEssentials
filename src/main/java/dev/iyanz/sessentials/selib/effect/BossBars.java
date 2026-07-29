package dev.iyanz.sessentials.selib.effect;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Helpers for Adventure boss bars. The returned {@link BossBar} is a live handle: mutate its
 * {@code name}/{@code progress}/{@code color} directly to update it, then {@link #hide} it when
 * done. The initial show is scheduled on the player's region thread for parity with the rest of
 * the plugin's entity work; boss-bar mutation and removal are thread-safe Adventure calls.
 */
public final class BossBars {

    private BossBars() {
    }

    /**
     * Creates and shows a boss bar to the player.
     *
     * @param plugin   the owning plugin
     * @param player   the recipient
     * @param name     the bar's title component
     * @param color    the bar colour
     * @param progress the fill fraction, clamped to {@code [0, 1]}
     * @return the live boss-bar handle (for later mutation / hiding)
     */
    public static BossBar show(SEssentialsPlugin plugin, Player player, Component name,
                               BossBar.Color color, float progress) {
        final float clamped = Math.max(BossBar.MIN_PROGRESS, Math.min(BossBar.MAX_PROGRESS, progress));
        final BossBar bar = BossBar.bossBar(name, clamped, color, BossBar.Overlay.PROGRESS);
        Schedulers.entity(plugin, player, () -> player.showBossBar(bar));
        return bar;
    }

    /**
     * Hides a previously-shown boss bar from the player.
     *
     * @param player the recipient
     * @param bar    the boss-bar handle returned by {@link #show}
     */
    public static void hide(Player player, BossBar bar) {
        player.hideBossBar(bar);
    }
}
