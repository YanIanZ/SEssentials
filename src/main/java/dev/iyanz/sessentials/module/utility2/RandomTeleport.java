package dev.iyanz.sessentials.module.utility2;

import java.util.concurrent.ThreadLocalRandom;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Picks a random, safe surface location within a radius of the origin and
 * teleports a player there ({@code /rtp}).
 *
 * <p>Folia-safe: a random point is very likely outside the sender's own loaded
 * region, so each candidate column's blocks are read on <em>that column's own</em>
 * region thread via Paper's {@link Bukkit#getRegionScheduler()} — never the
 * sender's. {@link Player#teleportAsync} is documented safe to call from any
 * thread, so the final teleport is issued straight from that same callback.</p>
 */
final class RandomTeleport {

    /** How many random columns to probe before giving up. */
    private static final int MAX_ATTEMPTS = 8;

    private RandomTeleport() {
    }

    /**
     * Attempts to teleport {@code player} to a random, safe surface location within
     * {@code radius} blocks of their current position, in their current world.
     *
     * @param plugin the owning plugin
     * @param player the player to teleport
     * @param radius the maximum distance (blocks) from the origin, on both axes
     */
    static void attempt(SEssentialsPlugin plugin, Player player, int radius) {
        Msg.info(player, "Searching for a safe location...");
        probe(plugin, player, player.getWorld(), radius, 1);
    }

    private static void probe(SEssentialsPlugin plugin, Player player, World world, int radius, int attempt) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = random.nextInt(radius * 2 + 1) - radius;
        int z = random.nextInt(radius * 2 + 1) - radius;
        Bukkit.getRegionScheduler().execute(plugin, world, x >> 4, z >> 4, () -> {
            Location destination = safeSurface(world, x, z);
            if (destination == null) {
                if (attempt >= MAX_ATTEMPTS) {
                    Msg.err(player, "Could not find a safe location nearby; try again.");
                } else {
                    probe(plugin, player, world, radius, attempt + 1);
                }
                return;
            }
            player.teleportAsync(destination, TeleportCause.COMMAND).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) {
                    Msg.ok(player, "Teleported to a random location.");
                } else {
                    Msg.err(player, "Random teleport failed.");
                }
            });
        });
    }

    /**
     * @param world the world to probe
     * @param x     the column's block X
     * @param z     the column's block Z
     * @return a location standing on top of the highest safe (solid, non-liquid,
     *         non-air) block at {@code (x, z)}, or {@code null} if the column has no
     *         safe surface (e.g. void, or the surface is water/lava)
     */
    private static Location safeSurface(World world, int x, int z) {
        Block highest = world.getHighestBlockAt(x, z);
        if (!SafeGround.isSafe(highest) || highest.getY() >= world.getMaxHeight() - 1) {
            return null;
        }
        return new Location(world, x + 0.5, highest.getY() + 1, z + 0.5);
    }
}
