package dev.iyanz.sessentials.module.buildtools;

import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * {@code /scan <block> [radius]}: counts blocks of a type inside a cube of
 * {@code radius} (default {@value #DEFAULT_RADIUS}, capped at {@value #MAX_RADIUS})
 * around the player and reports the coordinates of the nearest match.
 *
 * <p>Folia-safe: this is a pure read of blocks near the sender, which is safe on the
 * sender's own region thread — the thread a command executor already runs on — so no
 * scheduler hop is needed. Unloaded chunk columns are skipped so the scan never
 * forces a synchronous chunk load.</p>
 */
final class BlockScanner {

    /** Radius used when the command omits one. */
    static final int DEFAULT_RADIUS = 8;

    /** Hard radius cap (a 65-block cube; reads only, but still bounded). */
    static final int MAX_RADIUS = 32;

    private BlockScanner() {
    }

    /**
     * Counts {@code target} blocks around the player and reports the count and the
     * nearest match's coordinates.
     *
     * @param player the sending player (cube centre and report recipient)
     * @param target the block type to count
     * @param radius the cube radius in blocks (already validated, 1..{@value #MAX_RADIUS})
     */
    static void scan(Player player, Material target, int radius) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), centerY - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + radius);

        int count = 0;
        long nearestDistSq = Long.MAX_VALUE;
        int nearestX = 0;
        int nearestY = 0;
        int nearestZ = 0;

        int cachedChunkX = Integer.MIN_VALUE;
        int cachedChunkZ = Integer.MIN_VALUE;
        boolean cachedLoaded = false;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
                    cachedChunkX = chunkX;
                    cachedChunkZ = chunkZ;
                    cachedLoaded = world.isChunkLoaded(chunkX, chunkZ);
                }
                if (!cachedLoaded) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() != target) {
                        continue;
                    }
                    count++;
                    long dx = x - centerX;
                    long dy = y - centerY;
                    long dz = z - centerZ;
                    long distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestX = x;
                        nearestY = y;
                        nearestZ = z;
                    }
                }
            }
        }

        String name = BlockMaterials.pretty(target);
        if (count == 0) {
            Msg.info(player, "No " + name + " within " + radius + " blocks.");
            return;
        }
        Msg.ok(player, "Found " + count + " " + name + " within " + radius + " blocks.");
        Msg.value(player, "Nearest:", nearestX + ", " + nearestY + ", " + nearestZ);
    }
}
