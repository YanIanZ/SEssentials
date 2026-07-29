package dev.iyanz.sessentials.module.backpack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * The single live backpack per online owner. Reusing one {@link Inventory} instance
     * across every {@code /backpack} open makes the open GUI the single source of truth:
     * a re-open never reloads a fresh copy from the store, so the same items can never be
     * present in both a freshly loaded GUI and the player's real inventory at once.
     * Entries are dropped on quit (see {@link #handleQuit}).
     */
    private static final Map<UUID, BackpackMenu> LIVE = new ConcurrentHashMap<>();

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
        UUID id = player.getUniqueId();
        // Reuse the one live inventory if the player already has a backpack in memory; only
        // the very first open after login loads from the store. Same-UUID opens all run on
        // the player's own region thread, so this get-or-create is not racy.
        BackpackMenu menu = LIVE.computeIfAbsent(id, key -> {
            BackpackMenu created = new BackpackMenu(key, sizeFor(player));
            created.load(store);
            return created;
        });
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
     * Serialises every slot of the live inventory (empty slots included, as {@code null}
     * entries) into {@code store}. Called on close for durability and on quit before the
     * live instance is dropped. Reads are in-memory; {@link YamlStore#save()} flushes to
     * disk asynchronously.
     *
     * @param store the {@code backpack} data store
     */
    void save(YamlStore store) {
        ItemStack[] contents = inventory.getContents();
        List<ItemStack> serialized = new ArrayList<>(contents.length);
        for (ItemStack item : contents) {
            serialized.add(item);
        }
        store.set(owner + ".contents", serialized);
        store.save();
    }

    /**
     * Persists and forgets a player's live backpack when they disconnect, so the map does
     * not leak and a fresh instance is loaded from the store on their next login.
     *
     * @param id    the departing player's UUID
     * @param store the {@code backpack} data store
     */
    static void handleQuit(UUID id, YamlStore store) {
        BackpackMenu menu = LIVE.remove(id);
        if (menu != null) {
            menu.save(store);
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

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
