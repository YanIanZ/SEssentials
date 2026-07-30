package dev.iyanz.sessentials.module.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Rescues players from the void: cancels {@link DamageCause#VOID} damage and sends the
 * player to the server spawn instead of letting them die.
 *
 * <p>The destination mirrors {@code /spawn}: the location persisted by
 * {@code /setspawn} in the {@code spawn} data store is preferred, falling back to the
 * player's current world's vanilla spawn when none has been set.</p>
 *
 * <p>Void damage ticks repeatedly while a player keeps falling, so every tick is
 * cancelled but only one rescue teleport is kept in flight per player (tracked in a
 * concurrent set, cleared when the teleport future completes). This avoids stacking
 * redundant teleports and repeating the rescue message.</p>
 *
 * <p>Folia-safe: {@link EntityDamageEvent} fires on the damaged player's region thread,
 * where reading the player's world and calling
 * {@link Player#teleportAsync(Location, TeleportCause)} are both legal; no scheduler
 * hop is needed.</p>
 */
final class VoidRescueListener implements Listener {

    /** Name of the shared data store that holds the persisted server spawn. */
    private static final String SPAWN_STORE = "spawn";

    /** Path of the persisted spawn location inside the store. */
    private static final String SPAWN_PATH = "spawn";

    private final SEssentialsPlugin plugin;

    /** Players with a rescue teleport currently in flight. */
    private final Set<UUID> rescuing = ConcurrentHashMap.newKeySet();

    /**
     * Creates the listener.
     *
     * @param plugin the owning plugin, used to resolve the spawn data store
     */
    VoidRescueListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancels void damage to players and teleports them to spawn.
     *
     * @param event the damage event, ignored unless a player is taking void damage
     */
    @EventHandler(ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != DamageCause.VOID
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);

        UUID id = player.getUniqueId();
        if (!rescuing.add(id)) {
            return;
        }
        Location spawn = plugin.stores().get(SPAWN_STORE).getLocation(SPAWN_PATH);
        if (spawn == null) {
            spawn = player.getWorld().getSpawnLocation();
        }
        player.teleportAsync(spawn, TeleportCause.PLUGIN)
                .whenComplete((done, error) -> rescuing.remove(id));
        Msg.info(player, "You were rescued from the void.");
    }
}
