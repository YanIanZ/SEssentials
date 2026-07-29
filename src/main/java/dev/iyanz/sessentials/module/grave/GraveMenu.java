package dev.iyanz.sessentials.module.grave;

import java.util.List;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The retrieval GUI for a death grave — a plain, fully editable chest inventory whose
 * contents are the grave's items, checked out from the {@link GraveRegistry} when the
 * owner opens it. Clicks are never cancelled, so the player simply takes items out; on
 * close, {@link GraveCloseListener} hands whatever is left back to the registry. Because
 * the items are moved (never copied) between the grave, this inventory and the player,
 * no duplication is possible.
 *
 * <p>Only the owner ever opens their own grave: {@link #open} is only reachable via the
 * {@code /grave} command keyed on the sender's own UUID.</p>
 *
 * <p>Folia-safe: the inventory is opened on the viewer's own region thread via
 * {@link Schedulers#entity}.</p>
 */
final class GraveMenu implements InventoryHolder {

    /** Maximum chest size (a double chest); a death can never drop more than this. */
    private static final int MAX_SIZE = 54;
    /** Slots per chest row. */
    private static final int ROW = 9;

    private final UUID owner;
    private final Inventory inventory;

    private GraveMenu(UUID owner, int size) {
        this.owner = owner;
        // Safe to pass `this`: the inventory only keeps the reference for later
        // getHolder() calls and never invokes anything on it during construction.
        this.inventory = Bukkit.createInventory(this, size,
                MiniMessage.miniMessage().deserialize(Style.title("Your Grave")));
    }

    /**
     * Opens {@code player}'s grave, if they have one. The grave's items are checked out
     * of {@code registry} (draining it and flagging the grave as being viewed) and placed
     * into a freshly sized chest, which is then shown on the player's region thread. If
     * the player has no active grave, they are told so and nothing else happens.
     *
     * @param plugin   the owning plugin
     * @param player   the grave owner and viewer
     * @param registry the shared grave registry
     */
    static void open(SEssentialsPlugin plugin, Player player, GraveRegistry registry) {
        UUID id = player.getUniqueId();
        if (registry.isOpen(id)) {
            Msg.info(player, "Your grave is already open.");
            return;
        }
        List<ItemStack> items = registry.checkout(id);
        if (items == null) {
            Msg.info(player, "You don't have an active grave.");
            return;
        }
        GraveMenu menu = new GraveMenu(id, sizeFor(items.size()));
        for (int i = 0; i < items.size() && i < menu.inventory.getSize(); i++) {
            menu.inventory.setItem(i, items.get(i));
        }
        Schedulers.entity(plugin, player, () -> player.openInventory(menu.inventory));
    }

    /**
     * Chooses the smallest chest size (a multiple of nine, from 9 up to {@value #MAX_SIZE})
     * that holds {@code count} items.
     *
     * @param count the number of items to display
     * @return the chest size in slots
     */
    private static int sizeFor(int count) {
        int rows = Math.max(1, (count + ROW - 1) / ROW);
        return Math.min(MAX_SIZE, rows * ROW);
    }

    /** @return the UUID of the player this grave belongs to. */
    UUID owner() {
        return owner;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
