package dev.iyanz.sessentials.module.invtools;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Drives {@link ShulkerEditMenu}: writes its edited contents back into the held
 * shulker box item when the GUI is closed.
 */
final class ShulkerCloseListener implements Listener {

    /**
     * Writes the closed inventory back into the originating shulker box, if it was
     * a {@link ShulkerEditMenu}.
     *
     * @param event the close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ShulkerEditMenu menu) {
            menu.writeBack();
        }
    }
}
