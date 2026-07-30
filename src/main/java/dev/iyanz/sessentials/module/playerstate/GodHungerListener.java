package dev.iyanz.sessentials.module.playerstate;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Freezes hunger for players in god mode: cancels any {@link FoodLevelChangeEvent} whose
 * entity is a player currently in the {@link GodService} registry, so their food bar never
 * drops (matching CMI's passive god-mode behaviour).
 *
 * <p>Only registered when config {@code god.no-hunger} is enabled. The event fires on the
 * affected player's own region thread and the handler only cancels it — no entity mutation
 * — so no Folia scheduler hop is required.</p>
 *
 * @see GodService
 */
final class GodHungerListener implements Listener {

    private final GodService godService;

    /**
     * @param godService the shared god-mode registry consulted on every food change
     */
    GodHungerListener(GodService godService) {
        this.godService = godService;
    }

    /** Cancels the food change if the affected entity is a player currently in god mode. */
    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && godService.isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
