package dev.iyanz.sessentials.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes {@link InventoryClickEvent}s whose holder is a {@link Menu} back to that
 * menu, so each menu handles its own clicks (and cancels raw slot interaction).
 */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu) {
            menu.handleClick(event);
        }
    }
}
