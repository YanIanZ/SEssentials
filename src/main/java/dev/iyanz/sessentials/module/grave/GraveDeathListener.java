package dev.iyanz.sessentials.module.grave;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Captures a dying player's dropped items into a {@link Grave} instead of scattering
 * them on the ground.
 *
 * <p>Only acts when the feature is {@link GraveSettings#enabled() enabled}, the death is
 * <em>not</em> a keep-inventory death (another module — or the vanilla gamerule — may
 * have chosen to preserve the inventory), and there is actually something to store. It
 * copies {@link PlayerDeathEvent#getDrops()}, clears the event's drop list so nothing
 * scatters, and registers the grave keyed on the player's UUID. No world block is
 * modified — the death spot is only remembered — so terrain can't be griefed.</p>
 *
 * <p>Runs at {@link EventPriority#HIGH} so it observes the keep-inventory decision of
 * normal-priority listeners (e.g. the scavenger module) before deciding to act.
 * {@link PlayerDeathEvent} fires on the dying player's own region thread, so the grave
 * location and message need no scheduler hop.</p>
 *
 * @see GraveRegistry
 */
final class GraveDeathListener implements Listener {

    private final GraveSettings settings;
    private final GraveRegistry registry;

    /**
     * @param settings the live grave settings (enabled flag, expiry window)
     * @param registry the shared in-memory grave store
     */
    GraveDeathListener(GraveSettings settings, GraveRegistry registry) {
        this.settings = settings;
        this.registry = registry;
    }

    /** Moves the dying player's drops into a retrievable grave when eligible. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!settings.enabled() || event.getKeepInventory()) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        // Copy the drops out, then clear the event's list so nothing scatters on the ground.
        List<ItemStack> stored = new ArrayList<>(drops);
        drops.clear();

        Location where = player.getLocation().clone();
        long expiryAt = System.currentTimeMillis() + settings.expireMillis();
        registry.put(new Grave(player.getUniqueId(), where, stored, expiryAt));

        Msg.info(player, "Your items were stored in a grave.");
        Msg.value(player, "Grave location:",
                where.getBlockX() + ", " + where.getBlockY() + ", " + where.getBlockZ()
                        + " (" + worldName(where) + ")");
        Msg.info(player, "Use /grave to reclaim them.");
    }

    /** @return the grave world's name, or {@code "unknown"} if it has since unloaded. */
    private static String worldName(Location where) {
        return where.getWorld() == null ? "unknown" : where.getWorld().getName();
    }
}
