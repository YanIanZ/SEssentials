package dev.iyanz.sessentials.module.combat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Pins frozen players ({@link FrozenPlayers}) to their current horizontal position.
 *
 * <p>Only the horizontal position is enforced: if a frozen player's X or Z coordinate
 * would change, the move is rewritten back to the {@code from} location while keeping
 * the incoming yaw/pitch, so the player can still freely look around. This event
 * always fires on the moving player's own region thread, so no scheduler hop is
 * needed to rewrite their destination.</p>
 */
final class FreezeMoveListener implements Listener {

    private final FrozenPlayers frozenPlayers;

    /**
     * @param frozenPlayers the shared frozen-player registry consulted on every move
     */
    FreezeMoveListener(FrozenPlayers frozenPlayers) {
        this.frozenPlayers = frozenPlayers;
    }

    /** Cancels horizontal movement (but not looking around) for frozen players. */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozenPlayers.isFrozen(player.getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }
}
