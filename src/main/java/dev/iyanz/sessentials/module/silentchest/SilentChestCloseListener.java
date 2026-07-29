package dev.iyanz.sessentials.module.silentchest;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Drives {@link SilentChestView}: writes its edited contents back into the real
 * container block when the viewer closes the snapshot.
 */
final class SilentChestCloseListener implements Listener {

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to reach the container block's region thread */
    SilentChestCloseListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Writes the closed inventory back into its source container, if it was a
     * {@link SilentChestView}. No-op for any other inventory.
     *
     * @param event the close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SilentChestView view) {
            view.writeBack(plugin);
        }
    }
}
