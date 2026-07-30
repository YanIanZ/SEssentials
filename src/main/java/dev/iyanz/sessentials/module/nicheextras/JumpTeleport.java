package dev.iyanz.sessentials.module.nicheextras;

import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Teleports a player to the block they are looking at ({@code /jumpto}).
 *
 * <p>The target is resolved with an exact ray trace up to {@value #MAX_RANGE} blocks.
 * The command executor already runs on the sender's region thread, and the move uses
 * {@code teleportAsync}, so this is Folia-safe even when the destination lies in a
 * different region.</p>
 */
final class JumpTeleport {

    /** Maximum line-of-sight distance, in blocks. */
    private static final int MAX_RANGE = 64;

    private JumpTeleport() {
    }

    /**
     * Teleports {@code player} on top of the block they are looking at, keeping their
     * current facing direction. Reports an error if no block is within range.
     *
     * @param player the command sender (already on their own region thread)
     */
    static void jump(Player player) {
        Block target = player.getTargetBlockExact(MAX_RANGE);
        if (target == null) {
            Msg.err(player, "No block in sight within " + MAX_RANGE + " blocks.");
            return;
        }
        Location destination = target.getLocation().add(0.5, 1.0, 0.5);
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                Msg.ok(player, "Jumped to the targeted block.");
            } else {
                Msg.err(player, "Teleport failed.");
            }
        });
    }
}
