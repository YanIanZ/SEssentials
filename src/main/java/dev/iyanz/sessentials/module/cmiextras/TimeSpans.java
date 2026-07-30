package dev.iyanz.sessentials.module.cmiextras;

import java.util.Locale;

/**
 * Tiny duration codec for this module: parses compact span strings like {@code 30m},
 * {@code 1h}, {@code 7d} — including compounds such as {@code 1d12h30m} — into
 * milliseconds, and formats a millisecond span back into a short human-readable form
 * for ban and kick messages.
 */
final class TimeSpans {

    private TimeSpans() {
    }

    /**
     * Parses a span string into milliseconds. Accepted units: {@code s}, {@code m},
     * {@code h}, {@code d}, {@code w}; multiple {@code <number><unit>} tokens may be
     * concatenated ({@code "1d12h"}).
     *
     * @param input the span string, e.g. {@code "30m"} or {@code "1h30m"}
     * @return the total milliseconds, or {@code -1} if {@code input} is blank, malformed
     *         or overflows
     */
    static long parseMillis(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        long total = 0L;
        int i = 0;
        try {
            while (i < s.length()) {
                int start = i;
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
                if (i == start || i == s.length()) {
                    return -1L; // no digits, or a trailing number without a unit
                }
                long amount = Long.parseLong(s.substring(start, i));
                long unitMillis = switch (s.charAt(i)) {
                    case 's' -> 1_000L;
                    case 'm' -> 60_000L;
                    case 'h' -> 3_600_000L;
                    case 'd' -> 86_400_000L;
                    case 'w' -> 604_800_000L;
                    default -> -1L;
                };
                if (unitMillis < 0 || amount <= 0) {
                    return -1L;
                }
                total = Math.addExact(total, Math.multiplyExact(amount, unitMillis));
                i++;
            }
        } catch (NumberFormatException | ArithmeticException ex) {
            return -1L;
        }
        return total > 0 ? total : -1L;
    }

    /**
     * Formats a millisecond span compactly, e.g. {@code "1d 2h 30m"} or {@code "45s"}.
     * Sub-second spans round up to {@code "1s"}; non-positive spans format as {@code "0s"}.
     *
     * @param millis the span in milliseconds
     * @return the compact human-readable form
     */
    static String human(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long totalSeconds = (millis + 999) / 1000; // round up so "500ms" reads as 1s
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append("d ");
        }
        if (hours > 0) {
            out.append(hours).append("h ");
        }
        if (minutes > 0) {
            out.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            out.append(seconds).append("s");
        }
        return out.toString().trim();
    }
}
