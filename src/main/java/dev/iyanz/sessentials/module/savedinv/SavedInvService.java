package dev.iyanz.sessentials.module.savedinv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Persistence for named, per-player inventory snapshots, backed by the shared
 * {@code savedinv.yml} data store.
 *
 * <p>A snapshot is stored as a single 41-element list of native Bukkit
 * {@link ItemStack}s (which are {@code ConfigurationSerializable}, so
 * {@link YamlStore#set(String, Object)} persists them directly) under
 * {@code "<uuid>.<name>.contents"}: indices {@code 0-35} are the 36 main storage
 * slots ({@link PlayerInventory#getStorageContents()}), {@code 36-39} are the four
 * armour slots (helmet, chestplate, leggings, boots, in that order), and index
 * {@code 40} is the off-hand item. Empty slots are stored as {@code null}; the
 * off-hand slot is normalised from Bukkit's {@code AIR} sentinel to {@code null} on
 * save and back to {@code AIR} on load. Snapshot names are always lower-cased so
 * lookups, saves and deletes are case-insensitive.</p>
 */
final class SavedInvService {

    private static final int STORAGE_SIZE = 36;
    private static final int HELMET_INDEX = 36;
    private static final int CHESTPLATE_INDEX = 37;
    private static final int LEGGINGS_INDEX = 38;
    private static final int BOOTS_INDEX = 39;
    private static final int OFFHAND_INDEX = 40;
    private static final int TOTAL_SIZE = 41;

    private SavedInvService() {
    }

    /** @return the shared saved-inventory data store ({@code savedinv.yml}). */
    private static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get("savedinv");
    }

    /** @return the storage path for {@code uuid}'s snapshot named {@code name} (lower-cased). */
    private static String path(UUID uuid, String name) {
        return uuid + "." + name.toLowerCase(Locale.ROOT) + ".contents";
    }

    /** @return {@code uuid}'s saved snapshot names (already lower-case, as stored), sorted. */
    static List<String> names(SEssentialsPlugin plugin, UUID uuid) {
        List<String> names = new ArrayList<>(store(plugin).keys(uuid.toString()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** @return whether {@code uuid} already has a snapshot named {@code name}. */
    static boolean exists(SEssentialsPlugin plugin, UUID uuid, String name) {
        return store(plugin).contains(path(uuid, name));
    }

    /**
     * Snapshots {@code player}'s full inventory (36 storage slots, 4 armour slots and
     * the off-hand) as {@code name}, overwriting any existing snapshot of that name,
     * then persists the store.
     *
     * @param plugin the owning plugin
     * @param player the player whose inventory is snapshotted
     * @param name   the snapshot name (will be lower-cased)
     */
    static void save(SEssentialsPlugin plugin, Player player, String name) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> snapshot = new ArrayList<>(TOTAL_SIZE);
        for (ItemStack stack : inventory.getStorageContents()) {
            snapshot.add(stack);
        }
        snapshot.add(inventory.getHelmet());
        snapshot.add(inventory.getChestplate());
        snapshot.add(inventory.getLeggings());
        snapshot.add(inventory.getBoots());
        snapshot.add(normalize(inventory.getItemInOffHand()));

        YamlStore store = store(plugin);
        store.set(path(player.getUniqueId(), name), snapshot);
        store.save();
    }

    /**
     * Restores {@code player}'s snapshot named {@code name} into their inventory,
     * overwriting its current contents entirely (storage, armour and off-hand).
     *
     * @param plugin the owning plugin
     * @param player the player whose inventory is overwritten
     * @param name   the snapshot name (will be lower-cased)
     * @return {@code true} if a snapshot named {@code name} existed and was restored
     */
    static boolean load(SEssentialsPlugin plugin, Player player, String name) {
        UUID uuid = player.getUniqueId();
        if (!exists(plugin, uuid, name)) {
            return false;
        }
        List<?> raw = store(plugin).config().getList(path(uuid, name));
        if (raw == null) {
            return false;
        }

        ItemStack[] storage = new ItemStack[STORAGE_SIZE];
        for (int i = 0; i < STORAGE_SIZE; i++) {
            storage[i] = itemAt(raw, i);
        }
        PlayerInventory inventory = player.getInventory();
        inventory.setStorageContents(storage);
        inventory.setHelmet(itemAt(raw, HELMET_INDEX));
        inventory.setChestplate(itemAt(raw, CHESTPLATE_INDEX));
        inventory.setLeggings(itemAt(raw, LEGGINGS_INDEX));
        inventory.setBoots(itemAt(raw, BOOTS_INDEX));
        ItemStack offhand = itemAt(raw, OFFHAND_INDEX);
        inventory.setItemInOffHand(offhand == null ? new ItemStack(Material.AIR) : offhand);
        return true;
    }

    /**
     * Deletes {@code uuid}'s snapshot named {@code name} entirely, then persists the
     * store. No-op if it does not exist.
     *
     * @param plugin the owning plugin
     * @param uuid   the owning player's UUID
     * @param name   the snapshot name (will be lower-cased)
     */
    static void delete(SEssentialsPlugin plugin, UUID uuid, String name) {
        YamlStore store = store(plugin);
        store.remove(uuid + "." + name.toLowerCase(Locale.ROOT));
        store.save();
    }

    /** @return {@code null} if {@code item} is {@code null} or {@code AIR}, otherwise {@code item} unchanged. */
    private static ItemStack normalize(ItemStack item) {
        return (item == null || item.getType() == Material.AIR) ? null : item;
    }

    /** @return the {@link ItemStack} at {@code index} in {@code raw}, or {@code null} if absent/out of range/not an item. */
    private static ItemStack itemAt(List<?> raw, int index) {
        if (index < 0 || index >= raw.size()) {
            return null;
        }
        Object value = raw.get(index);
        return value instanceof ItemStack stack ? stack : null;
    }
}
