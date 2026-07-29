package dev.iyanz.sessentials.module.moderation;

import java.time.Duration;
import java.util.Locale;

/** Parses durations like {@code 30s}, {@code 15m}, {@code 2h}, {@code 7d}, {@code 1w}. */
final class Durations {

    private Durations() {
    }

    /** @return the duration, or {@code null} if unparseable. */
    static Duration parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        char unit = s.charAt(s.length() - 1);
        String num = s.substring(0, s.length() - 1);
        try {
            long n = Long.parseLong(num);
            return switch (unit) {
                case 's' -> Duration.ofSeconds(n);
                case 'm' -> Duration.ofMinutes(n);
                case 'h' -> Duration.ofHours(n);
                case 'd' -> Duration.ofDays(n);
                case 'w' -> Duration.ofDays(n * 7);
                default -> null;
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Formats a remaining-time in a compact {@code 1d 2h 3m} form. */
    static String human(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long s = millis / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) {
            sb.append(d).append("d ");
        }
        if (h > 0) {
            sb.append(h).append("h ");
        }
        if (m > 0) {
            sb.append(m).append("m ");
        }
        if (d == 0 && h == 0 && sec > 0) {
            sb.append(sec).append("s");
        }
        return sb.toString().trim();
    }
}
