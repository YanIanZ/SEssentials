package dev.iyanz.sessentials.module.playtime;

/**
 * Formats a whole-seconds duration as a short, human-readable string such as
 * {@code "3d 4h 12m"}. Leading zero-valued units are omitted, but once a larger unit
 * has been printed every smaller unit down to minutes is kept (so {@code "3d 0h 12m"}
 * reads as {@code "3d 0h 12m"}, not {@code "3d 12m"}).
 */
final class PlaytimeFormat {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE;
    private static final long SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR;

    private PlaytimeFormat() {
    }

    /**
     * @param totalSeconds elapsed play time, in whole seconds (negative treated as zero)
     * @return a {@code "Xd Yh Zm"}-style rendering, trimmed to the units that matter
     */
    static String format(long totalSeconds) {
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
}
