package dev.iyanz.sessentials.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Sounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for chest-menu GUIs. A menu is its own {@link InventoryHolder}, so
 * {@link MenuListener} can route clicks back to it. Subclasses populate slots in
 * {@link #build()} via {@link #set(int, ItemStack, Consumer)}; all raw clicks are
 * cancelled and dispatched to the registered per-slot handler.
 *
 * <p>Folia-safe: the inventory is opened on the viewer's region thread.
 */
public abstract class Menu implements InventoryHolder {

    protected static final MiniMessage MM = MiniMessage.miniMessage();

    protected final SEssentialsPlugin plugin;
    protected final Player viewer;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    protected Menu(SEssentialsPlugin plugin, Player viewer, Component title, int rows) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, Math.max(1, rows) * 9, title);
    }

    /** Populates the inventory. Called by {@link #open()}. */
    protected abstract void build();

    /** Places an item and (optionally) registers a click handler for its slot. */
    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        inventory.setItem(slot, item);
        if (onClick != null) {
            handlers.put(slot, onClick);
        }
    }

    /** Fills every empty slot with a filler item (no handler). */
    protected void fill(ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    /** Cancels the raw click and dispatches to the slot handler, if any. */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Consumer<InventoryClickEvent> handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            Sounds.click(viewer);
            handler.accept(event);
        }
    }

    /** Builds and opens the menu on the viewer's region thread. */
    public void open() {
        build();
        Schedulers.entity(plugin, viewer, () -> {
            viewer.openInventory(inventory);
            Sounds.open(viewer);
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
