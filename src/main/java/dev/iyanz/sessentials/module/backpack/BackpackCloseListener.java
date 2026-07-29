package dev.iyanz.sessentials.module.backpack;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Persists a {@link BackpackMenu} when its owner closes it. The (possibly edited)
 * contents are captured synchronously in the close event — which, on Folia, fires on
 * the owner's own region thread — and handed to the {@code backpack} store, whose
 * {@link YamlStore#save()} then writes them to disk asynchronously.
 *
 * <p>Every slot is stored, including empty ones (as {@code null} list entries), so
 * items are restored to the exact positions the player left them in.</p>
 */
final class BackpackCloseListener implements Listener {

    private final YamlStore store;

    /** @param store the {@code backpack} data store to persist closed backpacks into */
    BackpackCloseListener(YamlStore store) {
        this.store = store;
    }

    /**
     * Writes the closed inventory's contents back to the store, if it was a
     * {@link BackpackMenu}. No-op for any other inventory.
     *
     * @param event the inventory close event
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackMenu menu)) {
            return;
        }
        // Snapshot the contents now (synchronously, on the owner's region thread); the
        // actual disk write happens asynchronously inside store.save().
        ItemStack[] contents = event.getInventory().getContents();
        List<ItemStack> serialized = new ArrayList<>(contents.length);
        for (ItemStack item : contents) {
            serialized.add(item);
        }
        store.set(menu.owner() + ".contents", serialized);
        store.save();
    }
}
