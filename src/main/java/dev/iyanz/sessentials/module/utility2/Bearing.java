package dev.iyanz.sessentials.module.utility2;

/**
 * Converts a Minecraft yaw into an 8-point compass direction, and derives the yaw
 * that would face a given horizontal offset. Used by {@code /compass} to report the
 * sender's facing and the direction toward world spawn.
 *
 * <p>Minecraft's yaw convention: {@code 0} = south, {@code 90} = west,
 * {@code 180} = north, {@code 270} = east, increasing clockwise when viewed from
 * above.</p>
 */
final class Bearing {

    private static final String[] POINTS = {
            "South", "Southwest", "West", "Northwest", "North", "Northeast", "East", "Southeast"
    };

    private Bearing() {
    }

    /**
     * @param yaw a Minecraft yaw, in degrees
     * @return the 8-point compass direction (e.g. {@code "Northeast"}) for {@code yaw}
     */
    static String cardinal(float yaw) {
        float normalized = yaw % 360f;
        if (normalized < 0) {
            normalized += 360f;
        }
        int index = Math.round(normalized / 45f) % POINTS.length;
        return POINTS[index];
    }

    /**
     * Computes the yaw a player would need to face a horizontal offset, matching
     * Minecraft's yaw convention (see class doc).
     *
     * @param dx offset toward the target on the X axis (target.x - origin.x)
     * @param dz offset toward the target on the Z axis (target.z - origin.z)
     * @return the yaw, in degrees, pointing from the origin toward the offset
     */
    static float yawTo(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
