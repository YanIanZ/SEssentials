package dev.iyanz.sessentials.module.utility2;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Discards a {@link DisposeMenu}'s contents when it is closed. The inventory is
 * never written back to any player and never persisted, so this simply clears it —
 * whatever was left inside vanishes for good.
 */
final class DisposeListener implements Listener {

    /**
     * Clears the closed inventory if it was a {@link DisposeMenu}, so its contents
     * do not linger as an unreferenced object; nothing is written back anywhere.
     *
     * @param event the close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DisposeMenu) {
            event.getInventory().clear();
        }
    }
}
