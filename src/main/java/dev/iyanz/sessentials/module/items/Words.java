package dev.iyanz.sessentials.module.items;

import java.util.Locale;

/**
 * Formats {@code SNAKE_CASE}/{@code snake_case} identifiers (material names,
 * enchantment keys) as human-readable "Title Case" text for chat feedback.
 */
final class Words {

    private Words() {
    }

    /**
     * @param snakeCase an identifier such as {@code DIAMOND_SWORD} or {@code silk_touch}
     * @return the identifier rendered as {@code "Diamond Sword"} / {@code "Silk Touch"}
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
