package dev.iyanz.sessentials.module.attachedcommands;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Runs an item's attached command when its holder right-clicks with it in their
 * main hand. The interact event already fires on the clicking player's own region
 * thread, so {@link Player#performCommand(String)} can be called directly without a
 * scheduler hop.
 */
final class AttachedCommandListener implements Listener {

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to read an item's attached-command tag */
    AttachedCommandListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs the right-clicked item's attached command (main hand only), cancelling
     * the interaction and substituting {@code %player%} with the clicker's name.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        String command = AttachedCommandItems.readCommand(plugin, item);
        if (command == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        player.performCommand(command.replace("%player%", player.getName()));
    }
}
