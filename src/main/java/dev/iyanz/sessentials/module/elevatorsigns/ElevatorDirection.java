package dev.iyanz.sessentials.module.elevatorsigns;

import java.util.Locale;
import java.util.Optional;

/**
 * The travel direction encoded on an elevator sign's first line.
 *
 * <p>Recognised markers (case-insensitive, surrounding brackets optional):
 * {@code [Up]} / {@code [Lift Up]} for {@link #UP} and {@code [Down]} /
 * {@code [Lift Down]} for {@link #DOWN}.</p>
 */
enum ElevatorDirection {

    /** Travel towards higher Y coordinates. */
    UP(1),
    /** Travel towards lower Y coordinates. */
    DOWN(-1);

    private final int step;

    ElevatorDirection(int step) {
        this.step = step;
    }

    /** @return the per-block Y offset for this direction ({@code +1} up, {@code -1} down). */
    int step() {
        return step;
    }

    /**
     * Parses a sign's first line into an elevator direction.
     *
     * @param line the plain text of the sign's front line 0
     * @return the direction the marker names, or empty if the line is not a marker
     */
    static Optional<ElevatorDirection> parse(String line) {
        String text = line.trim().toLowerCase(Locale.ROOT);
        if (text.length() >= 2 && text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return switch (text) {
            case "up", "lift up" -> Optional.of(UP);
            case "down", "lift down" -> Optional.of(DOWN);
            default -> Optional.empty();
        };
    }

    /**
     * @param line the plain text of a sign's front line 0
     * @return whether the line is any elevator marker (up or down)
     */
    static boolean isMarker(String line) {
        return parse(line).isPresent();
    }
}
