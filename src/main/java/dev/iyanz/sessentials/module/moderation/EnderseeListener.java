package dev.iyanz.sessentials.module.moderation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Drives the {@link EnderseeMenu}: writes the edited contents back to the target on
 * close. Every one of the 27 GUI slots maps to a real ender-chest slot, so — unlike
 * {@link InvseeListener}, which must guard its inert armour-row labels — no click or
 * drag restrictions are needed; the close-time per-slot diff captures the final state.
 */
public final class EnderseeListener implements Listener {

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnderseeMenu menu) {
            menu.writeBack();
        }
    }
}
