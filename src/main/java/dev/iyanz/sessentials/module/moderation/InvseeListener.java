package dev.iyanz.sessentials.module.moderation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Drives the {@link InvseeMenu}: permits edits only on slots that map to real
 * inventory positions, blocks shift-clicks and drags that could land items in the
 * inert label slots, and writes the edited contents back to the target on close.
 */
public final class InvseeListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof InvseeMenu)) {
            return;
        }
        // Shift-clicks can move items into non-editable/label slots — block them.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        int raw = event.getRawSlot();
        // Clicks in the top (invsee) inventory are only allowed on editable slots.
        if (raw < event.getInventory().getSize() && !InvseeMenu.isEditable(raw)) {
            event.setCancelled(true);
        }
        // Clicks in the viewer's own inventory (raw >= size) are always allowed.
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof InvseeMenu)) {
            return;
        }
        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !InvseeMenu.isEditable(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof InvseeMenu menu) {
            menu.writeBack();
        }
    }
}
