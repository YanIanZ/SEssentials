package dev.iyanz.sessentials.module.buildtools;

import java.util.concurrent.atomic.AtomicInteger;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * {@code /replaceblock <from> <to> [radius]}: replaces every block of one type with
 * another inside a cube of {@code radius} (default {@value #DEFAULT_RADIUS}, capped
 * at {@value #MAX_RADIUS}) around the player.
 *
 * <p>Folia-safe: the cube can straddle region borders, so the work is split by chunk
 * column and each slice is dispatched to the region thread that owns that chunk via
 * {@link Bukkit#getRegionScheduler()}. Region ownership never splits a chunk, so every
 * block a slice touches belongs to the region it runs on. An atomic countdown joins
 * the slices; the last one to finish hops back onto the player's region thread via
 * {@link Schedulers#entity} to report the total.</p>
 */
final class BlockReplacer {

    /** Radius used when the command omits one. */
    static final int DEFAULT_RADIUS = 3;

    /** Hard radius cap (a 21-block cube — plenty for touch-up work). */
    static final int MAX_RADIUS = 10;

    private BlockReplacer() {
    }

    /**
     * Replaces {@code from} blocks with {@code to} in a cube around the player and
     * reports the number of blocks changed once every chunk slice has run.
     *
     * @param plugin the owning plugin, used for the region-thread dispatch
     * @param player the sending player (cube centre and report recipient)
     * @param from   the block type to replace
     * @param to     the block type to place
     * @param radius the cube radius in blocks (already validated, 1..{@value #MAX_RADIUS})
     */
    static void replace(SEssentialsPlugin plugin, Player player, Material from, Material to, int radius) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minY = Math.max(world.getMinHeight(), center.getBlockY() - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, center.getBlockY() + radius);
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        AtomicInteger replaced = new AtomicInteger();
        AtomicInteger pending = new AtomicInteger((maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1));

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                final int cx = chunkX;
                final int cz = chunkZ;
                Bukkit.getRegionScheduler().execute(plugin, world, cx, cz, () -> {
                    replaced.addAndGet(replaceSlice(world, cx, cz, minX, maxX, minY, maxY, minZ, maxZ, from, to));
                    if (pending.decrementAndGet() == 0) {
                        int total = replaced.get();
                        Schedulers.entity(plugin, player, () -> Msg.ok(player, "Replaced " + total + " "
                                + BlockMaterials.pretty(from) + " with " + BlockMaterials.pretty(to)
                                + " within " + radius + " blocks."));
                    }
                });
            }
        }
    }

    /**
     * Replaces matching blocks in the part of the cube that lies inside one chunk
     * column. Must run on the region thread owning that chunk.
     *
     * @return the number of blocks replaced in this slice
     */
    private static int replaceSlice(World world, int chunkX, int chunkZ,
                                    int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                    Material from, Material to) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return 0;
        }
        int fromX = Math.max(minX, chunkX << 4);
        int toX = Math.min(maxX, (chunkX << 4) + 15);
        int fromZ = Math.max(minZ, chunkZ << 4);
        int toZ = Math.min(maxZ, (chunkZ << 4) + 15);
        int count = 0;
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == from) {
                        block.setType(to);
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
