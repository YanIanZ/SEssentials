package dev.iyanz.sessentials.module.nicheextras;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Applies personal (client-side) time and weather overrides to every online player.
 *
 * <p>Personal time ({@code Player#setPlayerTime}) and personal weather
 * ({@code Player#setPlayerWeather}) only change what that player's client renders —
 * the actual world state is untouched. Each mutation is scheduled on the owning
 * player's region thread, as required on Folia.</p>
 */
final class PersonalEnvBroadcast {

    private PersonalEnvBroadcast() {
    }

    /**
     * Sets every online player's personal time to a fixed value.
     *
     * @param plugin the owning plugin (for scheduling)
     * @param sender the command sender to report back to
     * @param value  {@code day}, {@code night}, {@code noon}, {@code midnight}, or raw ticks
     * @return 1 if applied, 0 on bad input
     */
    static int timeAll(SEssentialsPlugin plugin, CommandSender sender, String value) {
        long ticks;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "day" -> ticks = 1000L;
            case "night" -> ticks = 13000L;
            case "noon" -> ticks = 6000L;
            case "midnight" -> ticks = 18000L;
            default -> {
                try {
                    ticks = Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    Msg.err(sender, "Unknown time: " + value + " (use day, night, noon, midnight or ticks).");
                    return 0;
                }
            }
        }
        if (ticks < 0L) {
            Msg.err(sender, "Time ticks must be zero or positive.");
            return 0;
        }
        final long fixed = ticks;
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, online, () -> online.setPlayerTime(fixed, false));
            count++;
        }
        Msg.ok(sender, "Personal time set to " + value.toLowerCase(Locale.ROOT) + " for "
                + count + (count == 1 ? " player." : " players."));
        return 1;
    }

    /**
     * Sets every online player's personal weather.
     *
     * @param plugin the owning plugin (for scheduling)
     * @param sender the command sender to report back to
     * @param type   {@code clear} or {@code rain}
     * @return 1 if applied, 0 on bad input
     */
    static int weatherAll(SEssentialsPlugin plugin, CommandSender sender, String type) {
        final WeatherType weather;
        switch (type.toLowerCase(Locale.ROOT)) {
            case "clear" -> weather = WeatherType.CLEAR;
            case "rain" -> weather = WeatherType.DOWNFALL;
            default -> {
                Msg.err(sender, "Unknown weather: " + type + " (use clear or rain).");
                return 0;
            }
        }
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            Schedulers.entity(plugin, online, () -> online.setPlayerWeather(weather));
            count++;
        }
        Msg.ok(sender, "Personal weather set to " + type.toLowerCase(Locale.ROOT) + " for "
                + count + (count == 1 ? " player." : " players."));
        return 1;
    }
}
