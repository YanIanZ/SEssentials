package dev.iyanz.sessentials.module.buildtools;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * {@code /point}: draws a short particle line from the player's eye along their look
 * direction — a visual pointer for "over there" moments. The line stops early at the
 * first solid block so it ends where the pointer "lands".
 *
 * <p>Folia-safe: particles are spawned within {@value #LENGTH} blocks of the sender,
 * inside the sender's own region, on the region thread the command executor already
 * runs on — no scheduler hop is needed.</p>
 */
final class ParticlePointer {

    /** Length of the pointer line in blocks. */
    static final double LENGTH = 8.0D;

    /** Distance between consecutive particles along the line. */
    private static final double STEP = 0.5D;

    private ParticlePointer() {
    }

    /**
     * Spawns the pointer line for the player. The particles themselves are the
     * feedback; no chat message is sent.
     *
     * @param player the sending player
     */
    static void point(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        World world = player.getWorld();
        for (double distance = 1.0D; distance <= LENGTH; distance += STEP) {
            Location at = eye.clone().add(
                    direction.getX() * distance,
                    direction.getY() * distance,
                    direction.getZ() * distance);
            world.spawnParticle(Particle.END_ROD, at, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (at.getBlock().getType().isSolid()) {
                break;
            }
        }
    }
}
