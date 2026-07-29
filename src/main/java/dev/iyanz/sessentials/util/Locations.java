package dev.iyanz.sessentials.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Serialises {@link Location}s to and from a compact {@code world,x,y,z,yaw,pitch}
 * string for storage in YAML data files.
 */
public final class Locations {

    private Locations() {
    }

    /**
     * @param loc a location (may be {@code null})
     * @return the encoded string, or {@code null} if {@code loc} or its world is null
     */
    public static String toString(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
    }

    /**
     * @param encoded a {@code world,x,y,z,yaw,pitch} string
     * @return the location, or {@code null} if malformed or the world is not loaded
     */
    public static Location fromString(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        String[] p = encoded.split(",");
        if (p.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(p[0]);
        if (world == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(p[1]);
            double y = Double.parseDouble(p[2]);
            double z = Double.parseDouble(p[3]);
            float yaw = p.length > 4 ? Float.parseFloat(p[4]) : 0f;
            float pitch = p.length > 5 ? Float.parseFloat(p[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
