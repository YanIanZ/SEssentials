package dev.iyanz.sessentials.module.portals;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Reads portal-wand clicks and records the clicking player's selection: left-clicking
 * a block sets position 1, right-clicking a block sets position 2 ({@link PortalSelections}).
 * The interact event fires on the clicking player's own region thread, so no scheduler
 * hop is required to message them.
 */
final class PortalWandListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final PortalSelections selections;

    /**
     * @param plugin     the owning plugin, used to identify the wand item
     * @param selections the shared selection store to record clicks into
     */
    PortalWandListener(SEssentialsPlugin plugin, PortalSelections selections) {
        this.plugin = plugin;
        this.selections = selections;
    }

    /** Records a wand click as position 1 (left-click) or position 2 (right-click), cancelling the interaction. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!PortalWand.isWand(plugin, item)) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        event.setCancelled(true);

        Location location = clicked.getLocation();
        if (action == Action.LEFT_CLICK_BLOCK) {
            selections.setPos1(player.getUniqueId(), location);
            Msg.ok(player, "Position 1 set: " + coords(location));
        } else {
            selections.setPos2(player.getUniqueId(), location);
            Msg.ok(player, "Position 2 set: " + coords(location));
        }
    }

    /** @return {@code "x, y, z"} block coordinates for a chat-friendly message. */
    private static String coords(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
}
