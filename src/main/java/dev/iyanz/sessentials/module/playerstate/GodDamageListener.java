package dev.iyanz.sessentials.module.playerstate;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Cancels all incoming damage for players currently in god mode.
 *
 * @see GodService
 */
final class GodDamageListener implements Listener {

    private final GodService godService;

    /**
     * @param godService the shared god-mode registry consulted on every damage event
     */
    GodDamageListener(GodService godService) {
        this.godService = godService;
    }

    /** Cancels the event if its victim is a player currently in god mode. */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godService.isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
