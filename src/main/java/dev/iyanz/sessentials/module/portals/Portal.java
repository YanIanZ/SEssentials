package dev.iyanz.sessentials.module.portals;

import org.bukkit.Location;

/**
 * An immutable portal definition: a named axis-aligned bounding box (AABB) within a
 * single world, plus the destination players walking into it are sent to.
 *
 * <p>{@code minX/minY/minZ} and {@code maxX/maxY/maxZ} are the componentwise minimum
 * and maximum of the two points a player selected with the portal wand, so the box's
 * corners are always the low/high extremes regardless of selection order. Containment
 * ({@link #contains(Location)}) is inclusive on every axis.</p>
 *
 * @param name        the portal's identifier (lower-case, as stored)
 * @param world       the name of the world this portal's box lives in
 * @param minX        minimum X of the bounding box (inclusive)
 * @param minY        minimum Y of the bounding box (inclusive)
 * @param minZ        minimum Z of the bounding box (inclusive)
 * @param maxX        maximum X of the bounding box (inclusive)
 * @param maxY        maximum Y of the bounding box (inclusive)
 * @param maxZ        maximum Z of the bounding box (inclusive)
 * @param destination the raw destination token: {@code "spawn"} or a warp name, resolved
 *                    at teleport-time (see {@link PortalDestinations})
 */
record Portal(String name, String world,
              double minX, double minY, double minZ,
              double maxX, double maxY, double maxZ,
              String destination) {

    /**
     * @param location the location to test
     * @return whether {@code location} is in this portal's world and inside its
     *         bounding box (inclusive on every axis)
     */
    boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
