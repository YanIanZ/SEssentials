package dev.iyanz.sessentials.module.silentchest;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Intercepts right-clicks on container blocks for players who have silent-chest
 * mode enabled: the interaction is cancelled before the vanilla container ever
 * opens — which is what drives the lid animation and open sound broadcast to
 * nearby players — and a {@link SilentChestView} snapshot is opened to the
 * clicking player instead.
 *
 * <p>Folia-safe: a {@code PlayerInteractEvent} for a block always fires on the
 * region thread that owns both the clicking player and the clicked block (a
 * player can only interact with blocks inside their own loaded region), so this
 * listener reads the block state and opens the snapshot directly, with no
 * scheduler hop.</p>
 */
final class SilentChestListener implements Listener {

    private final SilentChestService service;

    /** @param service the shared silent-mode toggle registry */
    SilentChestListener(SilentChestService service) {
        this.service = service;
    }

    /**
     * Cancels container right-clicks for silent-mode players and opens a snapshot
     * view instead. Ignored for other actions/hands, non-silent players and blocks
     * that are not a {@link Container} (e.g. a normal right-clickable block, or a
     * container-less block like a door).
     *
     * @param event the interact event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!service.isSilent(player.getUniqueId())) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (!(state instanceof Container container)) {
            return;
        }

        event.setCancelled(true);
        SilentChestView.open(player, block, container);
    }

    /** Clears the quitting player's silent-mode flag so their id does not leak from the set. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.remove(event.getPlayer().getUniqueId());
    }
}
