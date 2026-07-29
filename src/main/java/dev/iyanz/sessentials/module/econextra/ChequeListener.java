package dev.iyanz.sessentials.module.econextra;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Redeems {@code /cheque} items: right-clicking one in the main hand deposits its
 * tagged amount into the clicking player's balance and consumes one item. The
 * interact event fires on the clicking player's own region thread, so no scheduler
 * hop is required to touch their inventory/economy balance.
 */
final class ChequeListener implements Listener {

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to read the cheque's PDC amount and the economy hook */
    ChequeListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Redeems a right-clicked cheque (main hand only), cancelling the interaction. */
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
        Double amount = ChequeItems.readAmount(plugin, item);
        if (amount == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        EconomyHook eco = plugin.economy();
        if (!eco.available()) {
            Msg.err(player, "No economy provider found.");
            return;
        }
        eco.deposit(player.getUniqueId(), amount);
        consumeOne(player, item);
        Msg.value(player, "Cheque redeemed:", eco.format(amount));
    }

    /** Removes one unit of the redeemed cheque from the player's main hand. */
    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }
}
