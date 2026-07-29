package dev.iyanz.sessentials.module.grave;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;

/**
 * The in-memory store of active death graves, keyed by owner UUID (each player has at
 * most one grave at a time — a fresh death replaces any older grave).
 *
 * <p>The registry is touched from two threads: the owner's region thread (grave
 * creation on death, checkout/return around the retrieval GUI) and the async expiry
 * sweep. To keep those coordinated without item duplication, the state-changing steps
 * that must not interleave — checking a grave out to the GUI, returning it, and claiming
 * an expired grave for its final drop — are guarded by a single monitor. Everything done
 * inside that monitor is plain in-memory list/map work, so it never blocks on the
 * server.</p>
 *
 * <p>An owner whose grave is currently open (checked out to a {@link GraveMenu}) is
 * recorded in {@link #openOwners}; the sweep skips those graves so it can never drop
 * items that are, at that moment, sitting in the player's open screen.</p>
 */
final class GraveRegistry {

    private final Map<UUID, Grave> graves = new ConcurrentHashMap<>();
    private final Set<UUID> openOwners = ConcurrentHashMap.newKeySet();

    /**
     * Records a new grave for {@code owner}, replacing any previous one.
     *
     * @param grave the grave to store
     */
    void put(Grave grave) {
        graves.put(grave.owner(), grave);
    }

    /**
     * @param owner the owning player's UUID
     * @return that player's active grave, or {@code null} if they have none
     */
    Grave get(UUID owner) {
        return graves.get(owner);
    }

    /**
     * @param owner the owning player's UUID
     * @return {@code true} if that player currently has their grave GUI open (its items
     *         are checked out of the registry)
     */
    boolean isOpen(UUID owner) {
        return openOwners.contains(owner);
    }

    /**
     * Atomically checks {@code owner}'s grave out to the retrieval GUI: the grave's items
     * are drained from the registry-held list and returned for placement into the open
     * inventory, and the owner is flagged as viewing so the expiry sweep leaves the grave
     * alone. The grave entry itself remains in the registry so a re-open finds it.
     *
     * @param owner the owning player's UUID
     * @return the checked-out items (never {@code null}; possibly empty), or {@code null}
     *         if the player has no grave to open
     */
    synchronized List<ItemStack> checkout(UUID owner) {
        Grave grave = graves.get(owner);
        if (grave == null) {
            return null;
        }
        List<ItemStack> pulled = new ArrayList<>(grave.items());
        grave.items().clear();
        openOwners.add(owner);
        return pulled;
    }

    /**
     * Returns the still-remaining items to {@code owner}'s grave when the GUI closes and
     * clears the viewing flag. If the grave is now empty it is removed from the registry.
     *
     * @param owner     the owning player's UUID
     * @param remaining the items left in the closed inventory (nulls tolerated/skipped)
     * @return {@code true} if the grave was emptied and removed, {@code false} otherwise
     */
    synchronized boolean returnItems(UUID owner, List<ItemStack> remaining) {
        openOwners.remove(owner);
        Grave grave = graves.get(owner);
        if (grave == null) {
            return false;
        }
        for (ItemStack item : remaining) {
            if (item != null && !item.getType().isAir()) {
                grave.items().add(item);
            }
        }
        if (grave.isEmpty()) {
            graves.remove(owner);
            return true;
        }
        return false;
    }

    /**
     * Atomically claims {@code owner}'s grave for expiry: only succeeds if the grave is
     * present, not currently being viewed, and past its expiry instant. On success the
     * grave is removed from the registry and returned so its items can be dropped.
     *
     * @param owner the owning player's UUID
     * @param now   the current epoch-millis time
     * @return the removed grave, or {@code null} if it was absent, open, or not yet expired
     */
    synchronized Grave claimExpired(UUID owner, long now) {
        if (openOwners.contains(owner)) {
            return null;
        }
        Grave grave = graves.get(owner);
        if (grave == null || !grave.isExpired(now)) {
            return null;
        }
        graves.remove(owner);
        return grave;
    }

    /**
     * Atomically removes and returns every remaining grave, for the shutdown drop.
     * After this call the registry is empty.
     *
     * @return a snapshot list of all graves that were present
     */
    synchronized List<Grave> drainAll() {
        List<Grave> all = new ArrayList<>(graves.values());
        graves.clear();
        openOwners.clear();
        return all;
    }

    /** @return a snapshot of the owner UUIDs that currently have a grave. */
    List<UUID> owners() {
        return new ArrayList<>(graves.keySet());
    }
}
