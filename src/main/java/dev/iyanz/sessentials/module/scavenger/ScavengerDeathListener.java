package dev.iyanz.sessentials.module.scavenger;

import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Keeps a dying player's inventory, armor, and experience intact when they have
 * scavenger mode enabled (and still hold the {@code sessentials.scavenger}
 * permission).
 *
 * <p>{@link PlayerDeathEvent} fires on the dying player's own region thread, so no
 * scheduler hop is needed here.</p>
 *
 * @see ScavengerService
 */
final class ScavengerDeathListener implements Listener {

    private final ScavengerService service;

    /**
     * @param service the shared scavenger-mode registry consulted on every death
     */
    ScavengerDeathListener(ScavengerService service) {
        this.service = service;
    }

    /** Preserves inventory, level, and experience on death for eligible players. */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!service.isEnabled(player.getUniqueId()) || !player.hasPermission("sessentials.scavenger")) {
            return;
        }
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        Msg.info(player, "Scavenger mode kept your inventory and experience.");
    }
}
