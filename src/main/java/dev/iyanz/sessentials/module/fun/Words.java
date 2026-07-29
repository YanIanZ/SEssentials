package dev.iyanz.sessentials.module.fun;

import java.util.Locale;

/**
 * Formats {@code SNAKE_CASE}/{@code snake_case} identifiers (potion effect keys, dye
 * color names) as human-readable "Title Case" text for chat feedback.
 */
final class Words {

    private Words() {
    }

    /**
     * @param snakeCase an identifier such as {@code INSTANT_HEALTH} or {@code light_blue}
     * @return the identifier rendered as {@code "Instant Health"} / {@code "Light Blue"}
     */
    static String titleCase(String snakeCase) {
        String[] parts = snakeCase.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(snakeCase.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
