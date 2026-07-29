package dev.iyanz.sessentials.module.invtools;

import java.util.UUID;

import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

/**
 * An editable, 27-slot copy of a held shulker box's contents.
 *
 * <p>A shulker box item's own {@link ShulkerBox#getInventory()} is a snapshot view
 * that is not directly openable to a player, so this menu instead copies the 27
 * slots into a plain chest-sized {@link Inventory} of its own. The player edits that
 * copy; on close, {@link ShulkerCloseListener} calls {@link #writeBack()}, which
 * re-reads the item the player is still holding, verifies it is still the same
 * shulker box (same inventory slot, same material — see {@link #originalType}), and
 * only then copies the edited contents back into a fresh {@link ShulkerBox} block
 * state via {@link BlockStateMeta#setBlockState}.</p>
 *
 * <p>Folia-safe: {@link #open} is called from a player-only command executor, which
 * already runs on the sender's own region thread; the close event for a player
 * closing their own inventory likewise fires on that same thread, so no scheduler
 * hop is required here.</p>
 */
final class ShulkerEditMenu implements InventoryHolder {

    /** A shulker box's inventory is always 3 rows of 9. */
    private static final int SIZE = 27;

    private final UUID playerId;
    private final boolean offHand;
    private final int hotbarSlot;
    private final Material originalType;
    private final Inventory inventory;

    private ShulkerEditMenu(Player player, boolean offHand, int hotbarSlot, Material originalType) {
        this.playerId = player.getUniqueId();
        this.offHand = offHand;
        this.hotbarSlot = hotbarSlot;
        this.originalType = originalType;
        this.inventory = Bukkit.createInventory(this, SIZE,
                MiniMessage.miniMessage().deserialize(Style.title("Shulker Box")));
    }

    /**
     * Opens an editable copy of the shulker box {@code player} is holding.
     *
     * <p>The main hand is checked first, then the off hand. If neither hand holds a
     * shulker box item, the player is told so and nothing opens.</p>
     *
     * @param player the player opening the box
     */
    static void open(Player player) {
        PlayerInventory inv = player.getInventory();
        int heldSlot = inv.getHeldItemSlot();
        ItemStack mainHand = inv.getItem(heldSlot);
        ItemStack offHandItem = inv.getItemInOffHand();

        boolean useOffHand;
        ItemStack held;
        if (isShulkerBox(mainHand)) {
            held = mainHand;
            useOffHand = false;
        } else if (isShulkerBox(offHandItem)) {
            held = offHandItem;
            useOffHand = true;
        } else {
            Msg.err(player, "You must be holding a shulker box.");
            return;
        }

        ShulkerBox box = (ShulkerBox) ((BlockStateMeta) held.getItemMeta()).getBlockState();
        Inventory boxInventory = box.getInventory();

        ShulkerEditMenu menu = new ShulkerEditMenu(player, useOffHand, heldSlot, held.getType());
        for (int i = 0; i < SIZE; i++) {
            ItemStack item = boxInventory.getItem(i);
            menu.inventory.setItem(i, item == null ? null : item.clone());
        }
        player.openInventory(menu.inventory);
    }

    /**
     * Writes the edited 27 slots back into the held shulker item, if the player is
     * still online and still holding the same shulker box (matched by hand/slot and
     * material). Otherwise the edits are silently discarded.
     */
    void writeBack() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return; // player left — discard edits
        }
        PlayerInventory inv = player.getInventory();
        ItemStack current = offHand ? inv.getItemInOffHand() : inv.getItem(hotbarSlot);
        if (current == null || current.getType() != originalType || !isShulkerBox(current)) {
            return; // no longer the same shulker box — discard edits safely
        }

        BlockStateMeta meta = (BlockStateMeta) current.getItemMeta();
        BlockState state = meta.getBlockState();
        if (!(state instanceof ShulkerBox box)) {
            return;
        }
        Inventory boxInventory = box.getInventory();
        ItemStack[] edited = inventory.getContents();
        for (int i = 0; i < SIZE && i < boxInventory.getSize(); i++) {
            boxInventory.setItem(i, edited[i]);
        }
        meta.setBlockState(box);
        current.setItemMeta(meta);
        if (offHand) {
            inv.setItemInOffHand(current);
        } else {
            inv.setItem(hotbarSlot, current);
        }
    }

    /** @return {@code true} if {@code item} is a placed-block item backed by a {@link ShulkerBox} state. */
    private static boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
