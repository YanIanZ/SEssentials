package dev.iyanz.sessentials.module.invtools;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Guards {@link ShulkerEditMenu}: writes edits back into the held box on close, forgets
 * a viewer's menu on quit, and — while the menu is open — cancels any click or drag that
 * would move, drop or swap the box being edited. Together with the single-open guard in
 * {@link ShulkerEditMenu#open}, this ensures the box and its editable copy are never both
 * materialised, so items cannot be duped out of the copy while the box keeps its originals.
 */
final class ShulkerCloseListener implements Listener {

    /**
     * Writes the closed inventory back into the originating shulker box, if it was a
     * {@link ShulkerEditMenu}.
     *
     * @param event the close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ShulkerEditMenu menu) {
            ShulkerEditMenu.handleClose(menu);
        }
    }

    /**
     * Forgets a player's open shulker-edit menu when they disconnect.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ShulkerEditMenu.handleQuit(event.getPlayer().getUniqueId());
    }

    /**
     * Cancels clicks that would move, drop or swap the box being edited.
     *
     * @param event the click event
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ShulkerEditMenu menu && menu.blocks(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels drags that would deposit into the box being edited.
     *
     * @param event the drag event
     */
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShulkerEditMenu menu && menu.blocks(event)) {
            event.setCancelled(true);
        }
    }
}
