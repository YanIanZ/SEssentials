package dev.iyanz.sessentials.module.stats;

import java.util.Locale;

/**
 * Small formatting helpers that turn raw {@link org.bukkit.Statistic} values into
 * human-readable text for {@link StatsCommand}: a {@code "Xd Yh Zm"} duration for
 * play time, a metres/kilometres distance for movement statistics, and a one-decimal
 * health-points reading for the damage statistics.
 */
final class StatsFormat {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE;
    private static final long SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR;

    /** Centimetres in one metre ({@link org.bukkit.Statistic} movement stats are in centimetres). */
    private static final double CM_PER_METRE = 100.0;
    /** Metres in one kilometre. */
    private static final double METRES_PER_KM = 1000.0;

    /** {@link org.bukkit.Statistic#DAMAGE_DEALT}/{@code DAMAGE_TAKEN} are stored in tenths of 1 HP. */
    private static final double DAMAGE_UNITS_PER_HP = 10.0;

    private StatsFormat() {
    }

    /**
     * Formats a whole-seconds duration as a short, human-readable string such as
     * {@code "3d 4h 12m"}. Leading zero-valued units are omitted, but once a larger
     * unit has been printed every smaller unit down to minutes is kept.
     *
     * @param totalSeconds elapsed time in whole seconds (negative treated as zero)
     * @return a {@code "Xd Yh Zm"}-style rendering, trimmed to the units that matter
     */
    static String duration(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long days = seconds / SECONDS_PER_DAY;
        long hours = (seconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        long minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;

        StringBuilder out = new StringBuilder();
        if (days > 0L) {
            out.append(days).append("d ");
        }
        if (days > 0L || hours > 0L) {
            out.append(hours).append("h ");
        }
        out.append(minutes).append("m");
        return out.toString();
    }

    /**
     * Formats a centimetre distance (the unit {@code WALK_ONE_CM}/{@code SPRINT_ONE_CM}
     * and similar movement statistics use) as metres, or kilometres once the distance
     * reaches 1000 m.
     *
     * @param totalCentimetres travelled distance in centimetres (negative treated as zero)
     * @return e.g. {@code "842.0 m"} or {@code "12.34 km"}
     */
    static String distance(long totalCentimetres) {
        double metres = Math.max(0L, totalCentimetres) / CM_PER_METRE;
        if (metres >= METRES_PER_KM) {
            return String.format(Locale.ENGLISH, "%.2f km", metres / METRES_PER_KM);
        }
        return String.format(Locale.ENGLISH, "%.1f m", metres);
    }

    /**
     * Formats a raw {@code DAMAGE_DEALT}/{@code DAMAGE_TAKEN} statistic value (stored
     * in tenths of 1 HP) as health points, on the same 0-20 scale as
     * {@link org.bukkit.entity.Player#getHealth()}.
     *
     * @param rawStatValue the raw statistic value (negative treated as zero)
     * @return e.g. {@code "146.5 hp"}
     */
    static String damage(int rawStatValue) {
        double healthPoints = Math.max(0, rawStatValue) / DAMAGE_UNITS_PER_HP;
        return String.format(Locale.ENGLISH, "%.1f hp", healthPoints);
    }
}
