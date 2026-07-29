package dev.iyanz.sessentials.module.backpack;

import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Persists a {@link BackpackMenu} and manages its lifecycle. On close the (possibly
 * edited) contents are written to the {@code backpack} store for durability while the
 * live inventory is kept in memory as the single source of truth; on quit the live
 * inventory is persisted one last time and dropped so the per-player map cannot leak.
 *
 * <p>On Folia both events fire on the owner's own region thread; the store's
 * {@link YamlStore#save()} then flushes to disk asynchronously.</p>
 */
final class BackpackCloseListener implements Listener {

    private final YamlStore store;

    /** @param store the {@code backpack} data store to persist backpacks into */
    BackpackCloseListener(YamlStore store) {
        this.store = store;
    }

    /**
     * Writes the closed inventory's contents back to the store, if it was a
     * {@link BackpackMenu}. The live instance is left in memory for the next open, so a
     * re-open reuses the same inventory rather than reloading a fresh (dupe-able) copy.
     *
     * @param event the inventory close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BackpackMenu menu) {
            menu.save(store);
        }
    }

    /**
     * Persists and forgets the departing player's live backpack.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BackpackMenu.handleQuit(event.getPlayer().getUniqueId(), store);
    }
}
