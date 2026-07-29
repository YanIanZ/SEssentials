package dev.iyanz.sessentials.module.utility2;

import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * A 54-slot "trash bin" inventory for {@code /dispose}. It is never linked to any
 * player's real inventory or persisted to disk, so anything placed inside it simply
 * ceases to be referenced once the GUI closes; {@link DisposeListener} clears it on
 * close to make that disposal explicit rather than relying on garbage collection.
 */
final class DisposeMenu implements InventoryHolder {

    private final Inventory inventory;

    private DisposeMenu() {
        this.inventory = Bukkit.createInventory(this, 54,
                MiniMessage.miniMessage().deserialize(Style.title("Trash")));
    }

    /** @return a freshly created, empty trash inventory, ready to open for a player. */
    static Inventory create() {
        return new DisposeMenu().inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
