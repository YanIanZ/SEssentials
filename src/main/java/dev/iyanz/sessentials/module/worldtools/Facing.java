package dev.iyanz.sessentials.module.worldtools;

/**
 * Converts a Minecraft yaw into an 8-point compass direction, for display in
 * {@code /getpos}.
 *
 * <p>Minecraft's yaw convention: {@code 0} = south, {@code 90} = west,
 * {@code 180} = north, {@code 270} = east, increasing clockwise when viewed from
 * above.</p>
 */
final class Facing {

    private static final String[] POINTS = {
            "South", "Southwest", "West", "Northwest", "North", "Northeast", "East", "Southeast"
    };

    private Facing() {
    }

    /**
     * @param yaw a Minecraft yaw, in degrees
     * @return the 8-point compass direction (e.g. {@code "Northeast"}) the yaw faces
     */
    static String cardinal(float yaw) {
        float normalized = yaw % 360f;
        if (normalized < 0) {
            normalized += 360f;
        }
        int index = Math.round(normalized / 45f) % POINTS.length;
        return POINTS[index];
    }
}
