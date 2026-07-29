package dev.iyanz.sessentials.module.chestlock;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.block.Block;

/**
 * Storage helper for owner-only container locks. Every lock is a single flat entry in
 * {@code chestlock.yml} keyed by the block's encoded location ({@code world,x,y,z})
 * whose value is the owner's UUID string, e.g. {@code world,10,64,-30: 6f9…}.
 *
 * <p>All coordinates are block coordinates (integers), so the key never contains the
 * {@code '.'} YAML path separator. A world whose name itself contains a {@code '.'}
 * would break the flat key layout — an accepted limitation of this simple encoding.</p>
 */
final class ChestLocks {

    /** Data-store name; resolves to {@code chestlock.yml}. */
    static final String STORE = "chestlock";

    private ChestLocks() {
    }

    /** @return the flat storage key for {@code block}: {@code world,blockX,blockY,blockZ}. */
    static String encode(Block block) {
        return block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    /** @return the recorded owner of {@code block}, or {@code null} if it holds no lock. */
    static UUID owner(SEssentialsPlugin plugin, Block block) {
        String raw = store(plugin).getString(encode(block));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Records {@code owner} as the lock holder for {@code block} and persists the change. */
    static void lock(SEssentialsPlugin plugin, Block block, UUID owner) {
        YamlStore store = store(plugin);
        store.set(encode(block), owner.toString());
        store.save();
    }

    /** Removes any lock entry for {@code block} and persists the change. */
    static void unlock(SEssentialsPlugin plugin, Block block) {
        YamlStore store = store(plugin);
        store.remove(encode(block));
        store.save();
    }

    /** @return the shared {@code chestlock} store. */
    private static YamlStore store(SEssentialsPlugin plugin) {
        return plugin.stores().get(STORE);
    }
}
