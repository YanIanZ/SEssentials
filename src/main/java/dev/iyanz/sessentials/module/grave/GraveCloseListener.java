package dev.iyanz.sessentials.module.grave;

import java.util.Arrays;
import java.util.List;

import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Persists a {@link GraveMenu}'s remaining items back into the {@link GraveRegistry}
 * when the owner closes it. The (possibly emptied) contents are snapshotted from the
 * close event — which, on Folia, fires on the owner's own region thread — and handed to
 * {@link GraveRegistry#returnItems}. Whatever the player took is simply no longer in the
 * inventory, so it is not returned; nothing is copied.
 *
 * <p>If the player emptied the grave, the registry removes it and the player is told the
 * grave has been cleared.</p>
 */
final class GraveCloseListener implements Listener {

    private final GraveRegistry registry;

    /** @param registry the shared in-memory grave store to return remaining items to */
    GraveCloseListener(GraveRegistry registry) {
        this.registry = registry;
    }

    /**
     * Writes the closed grave inventory's remaining items back to the registry, if the
     * closed inventory was a {@link GraveMenu}. No-op for any other inventory.
     *
     * @param event the inventory close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GraveMenu menu)) {
            return;
        }
        // Snapshot now, synchronously, on the owner's region thread.
        List<ItemStack> remaining = Arrays.asList(event.getInventory().getContents());
        boolean emptied = registry.returnItems(menu.owner(), remaining);

        HumanEntity viewer = event.getPlayer();
        if (emptied) {
            Msg.ok(viewer, "You reclaimed everything from your grave.");
        }
    }
}
