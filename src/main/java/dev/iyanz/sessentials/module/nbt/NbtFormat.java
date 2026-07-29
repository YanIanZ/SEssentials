package dev.iyanz.sessentials.module.nbt;

import java.util.Locale;

/**
 * Small, shared formatting helper used by this module's report commands: a
 * {@code SOME_ENUM_NAME} / {@code some_key_name} → {@code "Some Enum Name"} title-caser
 * for material, entity-type, enchantment and potion-effect keys, regardless of the
 * input's original casing.
 */
final class NbtFormat {

    private NbtFormat() {
    }

    /**
     * @param name a snake_case name in any casing, e.g. {@code "GRASS_BLOCK"} or
     *             {@code "fire_resistance"}
     * @return the same name in title case with spaces, e.g. {@code "Grass Block"}
     */
    static String titleCase(String name) {
        String[] parts = name.toLowerCase(Locale.ROOT).split("[_ ]+");
        StringBuilder out = new StringBuilder();
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

    /** @return a value formatted to one decimal place (e.g. health/max-health readouts). */
    static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
