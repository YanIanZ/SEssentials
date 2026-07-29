package dev.iyanz.sessentials.module.placeholders;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import dev.iyanz.sessentials.SEssentialsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

/**
 * The {@code sessentials} PlaceholderAPI expansion, exposing a handful of common player
 * stats as {@code %sessentials_<param>%} placeholders:
 *
 * <ul>
 *   <li>{@code balance} — the player's formatted economy balance ({@code "0"} if no
 *       economy provider is hooked).</li>
 *   <li>{@code balance_raw} — the player's numeric economy balance.</li>
 *   <li>{@code homes_count} — how many homes the player has saved.</li>
 *   <li>{@code playtime} — the player's total play time, formatted as {@code "Xd Yh Zm"}
 *       (requires the player to be online; {@code "N/A"} otherwise).</li>
 *   <li>{@code world} — the name of the world the player is currently in (online only).</li>
 *   <li>{@code ping} — the player's current ping, in milliseconds (online only).</li>
 *   <li>{@code online} — the number of players currently online on the server.</li>
 * </ul>
 *
 * <p>Unknown parameters return {@code null}, per PlaceholderAPI convention. Most stats
 * read live entity state (world, ping, play time) and are therefore only available for
 * an online {@link Player}; economy balance and home count are read from disk-backed
 * services and work for offline players too.</p>
 *
 * <p><b>Folia note:</b> {@link #onRequest} is a synchronous callback mandated by
 * PlaceholderAPI's API contract (it must return a {@link String} immediately, with no
 * opportunity to hop schedulers and await a result), so entity reads here (world, ping,
 * play time) are performed directly on the calling thread rather than the target
 * player's region thread. This mirrors the constraint every PlaceholderAPI expansion
 * faces on Folia and is safe for simple getters, but callers embedding these
 * placeholders in code that also mutates the target entity should not assume the
 * region-thread affinity that the rest of this plugin otherwise guarantees.</p>
 */
public final class SEssentialsExpansion extends PlaceholderExpansion {

    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used to reach shared services (economy, stores)
     */
    public SEssentialsExpansion(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "sessentials";
    }

    @Override
    public String getAuthor() {
        return "Yan";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public List<String> getPlaceholders() {
        return List.of(
                "%sessentials_balance%",
                "%sessentials_balance_raw%",
                "%sessentials_homes_count%",
                "%sessentials_playtime%",
                "%sessentials_world%",
                "%sessentials_ping%",
                "%sessentials_online%");
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if ("online".equalsIgnoreCase(params)) {
            return String.valueOf(Bukkit.getOnlinePlayers().size());
        }
        if (player == null) {
            return null;
        }

        UUID uuid = player.getUniqueId();
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "balance" -> formattedBalance(uuid);
            case "balance_raw" -> String.valueOf(plugin.economy().balance(uuid));
            case "homes_count" -> String.valueOf(homesCount(uuid));
            case "playtime" -> formattedPlaytime(player);
            case "world" -> onlineOnly(player, p -> p.getWorld().getName());
            case "ping" -> onlineOnly(player, p -> String.valueOf(p.getPing()));
            default -> null;
        };
    }

    /** @return the formatted balance for {@code uuid}, or {@code "0"} if no economy is hooked. */
    private String formattedBalance(UUID uuid) {
        return plugin.economy().available()
                ? plugin.economy().format(plugin.economy().balance(uuid))
                : "0";
    }

    /** @return how many homes {@code uuid} has saved, per the home module's storage convention. */
    private int homesCount(UUID uuid) {
        return plugin.stores().get("home").keys(uuid + ".homes").size();
    }

    /**
     * @return {@code player}'s total play time formatted as {@code "Xd Yh Zm"}, or
     *         {@code "N/A"} if the player is not currently online (the play-time
     *         statistic is only readable on a live {@link Player}).
     */
    private static String formattedPlaytime(OfflinePlayer player) {
        return onlineOnly(player, p -> {
            long totalMinutes = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / TICKS_PER_MINUTE;
            long days = totalMinutes / (HOURS_PER_DAY * MINUTES_PER_HOUR);
            long hours = (totalMinutes % (HOURS_PER_DAY * MINUTES_PER_HOUR)) / MINUTES_PER_HOUR;
            long minutes = totalMinutes % MINUTES_PER_HOUR;
            return days + "d " + hours + "h " + minutes + "m";
        });
    }

    /**
     * Resolves {@code player} to a live {@link Player} and applies {@code fn}, or returns
     * {@code "N/A"} if the player is currently offline.
     */
    private static String onlineOnly(OfflinePlayer player, Function<Player, String> fn) {
        Player online = player.getPlayer();
        return online != null ? fn.apply(online) : "N/A";
    }
}
