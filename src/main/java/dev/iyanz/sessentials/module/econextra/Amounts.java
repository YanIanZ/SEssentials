package dev.iyanz.sessentials.module.econextra;

import java.util.Locale;

/**
 * Parses money amounts with optional {@code k}/{@code m}/{@code b}/{@code t} shorthand
 * (e.g. {@code 1.5m}). A standalone copy of the same parser used by the core
 * {@code economy} module, kept private to this package by design.
 */
final class Amounts {

    private Amounts() {
    }

    /** @return the parsed amount, or {@link Double#NaN} if invalid / non-positive. */
    static double parse(String input) {
        if (input == null) {
            return Double.NaN;
        }
        String s = input.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("_", "");
        if (s.isEmpty()) {
            return Double.NaN;
        }
        double multiplier = 1.0;
        char last = s.charAt(s.length() - 1);
        switch (last) {
            case 'k' -> multiplier = 1_000.0;
            case 'm' -> multiplier = 1_000_000.0;
            case 'b' -> multiplier = 1_000_000_000.0;
            case 't' -> multiplier = 1_000_000_000_000.0;
            default -> { }
        }
        if (multiplier != 1.0) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            double v = Double.parseDouble(s) * multiplier;
            return Double.isFinite(v) && v > 0 ? v : Double.NaN;
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
