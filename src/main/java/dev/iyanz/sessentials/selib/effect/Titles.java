package dev.iyanz.sessentials.selib.effect;

import java.time.Duration;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/**
 * Folia-safe helpers for on-screen titles. Each call hops to the player's region thread via
 * {@link Schedulers#entity} before invoking {@link Player#showTitle(Title)}.
 */
public final class Titles {

    private Titles() {
    }

    /**
     * Shows a title/subtitle with the client's default timings.
     *
     * @param plugin   the owning plugin
     * @param player   the recipient
     * @param title    the main title component
     * @param subtitle the subtitle component
     */
    public static void show(SEssentialsPlugin plugin, Player player, Component title, Component subtitle) {
        final Title payload = Title.title(title, subtitle);
        Schedulers.entity(plugin, player, () -> player.showTitle(payload));
    }

    /**
     * Shows a title/subtitle with explicit fade/stay timings.
     *
     * @param plugin   the owning plugin
     * @param player   the recipient
     * @param title    the main title component
     * @param subtitle the subtitle component
     * @param fadeIn   fade-in duration
     * @param stay     on-screen duration
     * @param fadeOut  fade-out duration
     */
    public static void show(SEssentialsPlugin plugin, Player player, Component title, Component subtitle,
                            Duration fadeIn, Duration stay, Duration fadeOut) {
        final Title payload = Title.title(title, subtitle, Title.Times.times(fadeIn, stay, fadeOut));
        Schedulers.entity(plugin, player, () -> player.showTitle(payload));
    }

    /**
     * Clears any currently-displayed title on the player's region thread.
     *
     * @param plugin the owning plugin
     * @param player the recipient
     */
    public static void clear(SEssentialsPlugin plugin, Player player) {
        Schedulers.entity(plugin, player, player::clearTitle);
    }
}
