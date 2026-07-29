package dev.iyanz.sessentials.module.grave;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * A single death grave: the dropped items a player left behind, remembered in memory
 * so the owner can reclaim them with {@code /grave} before they expire.
 *
 * <p>The {@link #items()} list is the <em>single source of truth</em> for what the
 * grave holds while it is not being viewed. When the owner opens the retrieval GUI the
 * items are checked out into a {@link GraveMenu} inventory and this list is emptied;
 * on close, whatever the player did not take is written straight back here. Items are
 * therefore only ever moved between this list, the open inventory and the player — never
 * copied — so no duplication can occur.</p>
 *
 * <p>Graves live only in memory (see {@link GraveRegistry}) and do not survive a
 * server restart; {@link GraveModule#disable} drops any remaining items to the ground
 * so nothing is silently lost on shutdown.</p>
 *
 * @param owner    the UUID of the player who died and owns this grave
 * @param location where the player died (used as the drop point on expiry)
 * @param items    the remaining items, mutated in place as they are checked out
 * @param expiryAt the epoch-millis instant at which this grave becomes eligible for expiry
 */
record Grave(UUID owner, Location location, List<ItemStack> items, long expiryAt) {

    /** @return {@code true} if this grave holds no items. */
    boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * @param now the current epoch-millis time
     * @return {@code true} if this grave has lived past its expiry instant
     */
    boolean isExpired(long now) {
        return now >= expiryAt;
    }
}
