package dev.iyanz.sessentials.module.cmdcontrol;

import java.util.Locale;

/** Formats a remaining cooldown duration compactly for player-facing messages. */
final class CooldownFormat {

    private CooldownFormat() {
    }

    /**
     * @param millis remaining cooldown, in milliseconds (rounded up to the next whole second)
     * @return a compact rendering such as {@code "1h 2m"}, {@code "2m 5s"}, or {@code "5s"}
     */
    static String compact(long millis) {
        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return minutes > 0L
                    ? String.format(Locale.ROOT, "%dh %dm", hours, minutes)
                    : String.format(Locale.ROOT, "%dh", hours);
        }
        if (minutes > 0L) {
            return seconds > 0L
                    ? String.format(Locale.ROOT, "%dm %ds", minutes, seconds)
                    : String.format(Locale.ROOT, "%dm", minutes);
        }
        return seconds + "s";
    }
}
