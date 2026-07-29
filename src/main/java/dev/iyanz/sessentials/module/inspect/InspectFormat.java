package dev.iyanz.sessentials.module.inspect;

import java.util.Locale;

import org.bukkit.Location;

/**
 * Small, shared formatting helpers used by every report command in this module: a
 * human-readable {@code "world (x, y, z)"} location description, and a
 * {@code SOME_ENUM_NAME} → {@code "Some Enum Name"} title-caser for material/biome/
 * entity/gamemode names.
 */
final class InspectFormat {

    private InspectFormat() {
    }

    /**
     * @param location a location, possibly with an unloaded/null world
     * @return {@code "world (x, y, z)"}, or a fallback if the location can't be resolved
     */
    static String location(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unavailable";
        }
        return location.getWorld().getName() + " (" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /**
     * @param enumName an upper-snake-case enum name, e.g. {@code "GRASS_BLOCK"}
     * @return the same name in title case with spaces, e.g. {@code "Grass Block"}
     */
    static String titleCase(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
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
