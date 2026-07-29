package dev.iyanz.sessentials.module.backpack;

import java.util.List;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A player's persistent virtual backpack — a standalone chest inventory whose contents
 * live in the {@code backpack} data store rather than in any real block or item. The
 * inventory is fully editable (clicks are never cancelled); its contents are loaded
 * from the store when opened and written back when closed by
 * {@link BackpackCloseListener}.
 *
 * <p>The number of slots is chosen per-player from their permissions (see
 * {@link #sizeFor(Player)}), so ranks can grant larger backpacks. Contents are stored
 * per slot — empty slots included — under {@code "<uuid>.contents"} as a
 * {@link ItemStack} list, so items return to the exact slots the player left them in.</p>
 *
 * <p>Folia-safe: the backpack is opened on the viewer's own region thread (via
 * {@link Schedulers#entity}), and the store save on close is asynchronous.</p>
 */
final class BackpackMenu implements InventoryHolder {

    /** The default backpack size (slots) when no size permission is held. */
    static final int DEFAULT_SIZE = 27;
    /**
     * Candidate chest sizes a {@code sessentials.backpack.<n>} permission may unlock,
     * listed largest-first so {@link #sizeFor(Player)} can return the largest match.
     */
    private static final int[] SIZES = {54, 45, 36, 27, 18, 9};

    private final UUID owner;
    private final Inventory inventory;

    private BackpackMenu(UUID owner, int size) {
        this.owner = owner;
        // Safe to pass `this`: the created inventory only keeps the reference for later
        // getHolder() calls and never invokes anything on it.
        this.inventory = Bukkit.createInventory(this, size,
                MiniMessage.miniMessage().deserialize(Style.title("Backpack")));
    }

    /**
     * Opens {@code player}'s backpack, sized from their permissions and populated from
     * {@code store}. The open is scheduled on the player's own region thread, so this is
     * safe to call from anywhere.
     *
     * @param plugin the owning plugin
     * @param player the backpack owner and viewer
     * @param store  the {@code backpack} data store
     */
    static void open(SEssentialsPlugin plugin, Player player, YamlStore store) {
        BackpackMenu menu = new BackpackMenu(player.getUniqueId(), sizeFor(player));
        menu.load(store);
        Schedulers.entity(plugin, player, () -> player.openInventory(menu.inventory));
    }

    /**
     * Populates this backpack from the store, placing each saved item back into its
     * original slot. Reads are in-memory (the store is already loaded), so this is safe
     * on the caller's region thread.
     *
     * @param store the {@code backpack} data store
     */
    private void load(YamlStore store) {
        List<?> raw = store.config().getList(owner + ".contents");
        if (raw == null) {
            return;
        }
        int size = inventory.getSize();
        for (int i = 0; i < raw.size() && i < size; i++) {
            if (raw.get(i) instanceof ItemStack stack) {
                inventory.setItem(i, stack);
            }
        }
    }

    /**
     * Selects a backpack size from {@code player}'s permissions: the largest {@code n}
     * for which they hold {@code sessentials.backpack.<n>}, or {@link #DEFAULT_SIZE} if
     * they hold no such size permission.
     *
     * @param player the backpack owner
     * @return the backpack size in slots (a multiple of nine between 9 and 54)
     */
    static int sizeFor(Player player) {
        for (int size : SIZES) {
            if (player.hasPermission("sessentials.backpack." + size)) {
                return size;
            }
        }
        return DEFAULT_SIZE;
    }

    /** @return the UUID of the player this backpack belongs to. */
    UUID owner() {
        return owner;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
