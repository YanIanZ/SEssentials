package dev.iyanz.sessentials.selib.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes inventory events whose holder is a SELIB {@link Menu} back to that menu: clicks to
 * {@link Menu#handleClick(InventoryClickEvent)}, drags to {@link Menu#handleDrag(InventoryDragEvent)}
 * and closes to {@link Menu#handleClose(InventoryCloseEvent)}.
 *
 * <p>Distinct from the legacy {@code dev.iyanz.sessentials.gui.MenuListener}: each listener only
 * matches its own holder type, so the two can coexist during migration.</p>
 */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu) {
            menu.handleClose(event);
        }
    }
}
