package dev.iyanz.sessentials.module.protection;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 * Blocks naturally occurring phantom spawns so players who skip sleeping are never
 * harassed by phantoms.
 *
 * <p>Only ambient world spawns are blocked ({@link SpawnReason#NATURAL} and
 * {@link SpawnReason#PATROL}); deliberate sources such as spawn eggs, commands,
 * spawners and plugin spawns still work.</p>
 *
 * <p>Folia-safe: {@link CreatureSpawnEvent} fires on the region thread owning the
 * spawn location, and cancelling the event there is legal.</p>
 */
final class PhantomBlockListener implements Listener {

    /**
     * Cancels natural and patrol phantom spawns.
     *
     * @param event the spawn event, ignored unless an ambient phantom spawn
     */
    @EventHandler(ignoreCancelled = true)
    public void onPhantomSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.PHANTOM) {
            return;
        }
        SpawnReason reason = event.getSpawnReason();
        if (reason == SpawnReason.NATURAL || reason == SpawnReason.PATROL) {
            event.setCancelled(true);
        }
    }
}
