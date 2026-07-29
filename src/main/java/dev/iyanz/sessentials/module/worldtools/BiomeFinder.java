package dev.iyanz.sessentials.module.worldtools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Searches outward from a player for the nearest world column whose biome matches a
 * target, for {@code /findbiome}.
 *
 * <p>A block-by-block scan out to thousands of blocks would be far too expensive, so
 * this samples a bounded set of points instead: concentric rings every
 * {@link #RING_STEP} blocks (nearest ring first), each ring probed at
 * {@link #ANGLE_SAMPLES} evenly spaced directions, out to {@link #MAX_RADIUS} blocks.
 * That keeps the whole search under a thousand probes worst case. It is a heuristic,
 * not an exhaustive scan: a small biome pocket that falls between two sampled angles —
 * most likely at the outer rings, where samples are sparser — can be missed.</p>
 *
 * <p>Folia-safe: a sampled column is very likely outside the searching player's own
 * loaded region, so every biome read is hopped onto <em>that column's</em> region
 * thread via Paper's {@link Bukkit#getRegionScheduler()} (mirroring
 * {@code RandomTeleport} in {@code module.utility2}), never the player's. The result is
 * reported back on the player's own region thread via {@link Schedulers#entity}, since
 * by the time a distant column's callback runs, the player is no longer guaranteed to
 * still be on the thread that issued the command.</p>
 */
final class BiomeFinder {

    /** Radius step between successive rings, in blocks. */
    private static final int RING_STEP = 64;

    /** Outer search bound, in blocks. */
    private static final int MAX_RADIUS = 4000;

    /** Directions sampled per ring. */
    private static final int ANGLE_SAMPLES = 16;

    /** The full ordered set of sample offsets, nearest ring first; computed once. */
    private static final List<int[]> OFFSETS = buildOffsets();

    private BiomeFinder() {
    }

    /**
     * Searches outward from {@code player}'s current position for the nearest column
     * whose biome is {@code target}, reporting the result (or "not found") to the
     * player once the search concludes.
     *
     * @param plugin the owning plugin
     * @param player the searching player
     * @param target the biome to look for
     */
    static void search(SEssentialsPlugin plugin, Player player, Biome target) {
        String label = target.getKey().getKey().replace('_', ' ');
        Msg.info(player, "Searching for the nearest " + label + "...");

        World world = player.getWorld();
        int originX = player.getLocation().getBlockX();
        int originZ = player.getLocation().getBlockZ();
        int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, player.getLocation().getBlockY()));

        probe(plugin, player, world, target, label, 0, originX, y, originZ);
    }

    /** Builds the ordered list of (dx, dz) sample offsets, nearest ring first. */
    private static List<int[]> buildOffsets() {
        List<int[]> points = new ArrayList<>();
        points.add(new int[] {0, 0});
        int rings = MAX_RADIUS / RING_STEP;
        for (int ring = 1; ring <= rings; ring++) {
            int radius = ring * RING_STEP;
            for (int a = 0; a < ANGLE_SAMPLES; a++) {
                double angle = (2 * Math.PI * a) / ANGLE_SAMPLES;
                int dx = (int) Math.round(radius * Math.cos(angle));
                int dz = (int) Math.round(radius * Math.sin(angle));
                points.add(new int[] {dx, dz});
            }
        }
        return points;
    }

    private static void probe(SEssentialsPlugin plugin, Player player, World world, Biome target, String label,
            int index, int originX, int y, int originZ) {
        if (index >= OFFSETS.size()) {
            Schedulers.entity(plugin, player, () ->
                    Msg.err(player, "No " + label + " found within " + MAX_RADIUS + " blocks."));
            return;
        }
        int[] offset = OFFSETS.get(index);
        int x = originX + offset[0];
        int z = originZ + offset[1];

        Bukkit.getRegionScheduler().execute(plugin, world, x >> 4, z >> 4, () -> {
            Biome found = world.getBiome(x, y, z);
            if (found.equals(target)) {
                double distance = Math.sqrt((double) offset[0] * offset[0] + (double) offset[1] * offset[1]);
                Schedulers.entity(plugin, player, () -> {
                    Msg.ok(player, "Found " + label + " nearby.");
                    Msg.value(player, "Location:", String.format(Locale.ROOT, "%d, %d, %d (~%.0f blocks away)",
                            x, y, z, distance));
                });
                return;
            }
            probe(plugin, player, world, target, label, index + 1, originX, y, originZ);
        });
    }
}
