package dev.iyanz.sessentials.module.teleport;

import dev.iyanz.sessentials.scheduler.Schedulers;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;

/**
 * Shared cross-region teleport helper. Reading another entity's {@link Location} is
 * only safe on Folia from that entity's own region thread, while
 * {@link Player#teleportAsync} is safe to call from any thread. This helper hops onto
 * the destination's thread to read its location, then hands the (cloned) location to
 * the mover's async teleport from there.
 */
final class Teleports {

    private Teleports() {
    }

    /**
     * Teleports {@code mover} to {@code anchor}'s current location.
     *
     * @param plugin the owning plugin
     * @param mover  the player who will be moved
     * @param anchor the player whose current location is the destination
     */
    static void moveToPlayer(Plugin plugin, Player mover, Player anchor) {
        Schedulers.entity(plugin, anchor, () -> {
            Location destination = anchor.getLocation().clone();
            mover.teleportAsync(destination, TeleportCause.COMMAND);
        });
    }
}
