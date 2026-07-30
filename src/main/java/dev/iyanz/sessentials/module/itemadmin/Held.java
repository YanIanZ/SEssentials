package dev.iyanz.sessentials.module.itemadmin;

import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Shared held-item access for the itemadmin commands: fetches the sender's
 * main-hand item, erroring uniformly when the hand is empty.
 */
final class Held {

    private Held() {
    }

    /**
     * Returns the player's main-hand item, or {@code null} (after messaging the
     * player) if they are holding nothing.
     *
     * @param player the command sender
     * @return the held stack, or {@code null} if the hand is empty
     */
    static ItemStack mainHandOrError(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(player, "You are not holding an item.");
            return null;
        }
        return hand;
    }
}
