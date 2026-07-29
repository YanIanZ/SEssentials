package dev.iyanz.sessentials.module.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Drives the {@link TradeMenu} GUIs. Its job is to guarantee that items can only ever
 * enter or leave a player's <em>own</em> offer slots, never the read-only mirror,
 * divider, or control widgets — because only offer slots are drained on completion, an
 * item stranded elsewhere would be a duplication or loss bug.
 *
 * <p>To that end it cancels every bulk/ambiguous inventory action (shift-click,
 * double-click collect, hotbar swaps, number-key moves) and any click or drag that
 * targets a non-offer top slot. The confirm button is handled specially. Closing or
 * quitting hands off to {@link TradeSession}, which restores items safely.</p>
 */
public final class TradeListener implements Listener {

    private final TradeManager manager;

    /**
     * @param manager the shared trade registry (for the quit lookup)
     */
    public TradeListener(TradeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeMenu menu)) {
            return;
        }
        TradeSession session = menu.session();
        int raw = event.getRawSlot();
        int topSize = event.getInventory().getSize();

        // The confirm / ready toggle.
        if (raw == TradeMenu.CONFIRM_SLOT) {
            event.setCancelled(true);
            session.onConfirmToggled(menu.side());
            return;
        }

        // Block every action that could shuttle items in bulk or to an ambiguous slot.
        InventoryAction action = event.getAction();
        if (event.getClick() == ClickType.DOUBLE_CLICK
                || event.isShiftClick()
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_SWAP) {
            event.setCancelled(true);
            return;
        }

        if (raw < topSize) {
            // A click inside the trade GUI: only the player's own offer slots are editable.
            if (!TradeMenu.isOfferSlot(raw)) {
                event.setCancelled(true);
                return;
            }
            // Legitimate offer edit — let it apply, then re-sync the mirror/confirm state.
            session.onOfferChanged(menu.side());
        }
        // Clicks in the player's own inventory (raw >= topSize) fall through untouched;
        // bulk moves into the GUI were already blocked above.
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeMenu menu)) {
            return;
        }
        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !TradeMenu.isOfferSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        // Only offer slots (and the player's own inventory) were touched — allow, re-sync.
        menu.session().onOfferChanged(menu.side());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TradeMenu menu
                && event.getPlayer() instanceof Player player) {
            menu.session().endBy(player, "closed the trade.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TradeSession session = manager.sessionOf(event.getPlayer().getUniqueId());
        if (session != null) {
            session.endBy(event.getPlayer(), "left the server; trade cancelled.");
        }
    }
}
