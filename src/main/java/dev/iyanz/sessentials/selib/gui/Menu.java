package dev.iyanz.sessentials.selib.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Sounds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * SELIB base class for chest-menu GUIs. A menu is its own {@link InventoryHolder}, so
 * {@link MenuListener} can route clicks, drags and closes back to it. Subclasses populate
 * slots in {@link #build()} via {@link #set(int, ItemStack, Consumer)}; by default every
 * raw click is cancelled and dispatched to the registered per-slot handler.
 *
 * <p>Menus may declare <em>editable</em> slots via {@link #editable(int)} /
 * {@link #editable(int, int)}. Editable top-inventory slots are never cancelled and never
 * routed, so the player can freely move items in and out — this lets storage-style GUIs
 * (backpack / shulker / invsee) reuse this base class instead of hand-rolling an
 * {@link InventoryHolder}. Drags are cancelled only when they touch a locked top slot.</p>
 *
 * <p>Folia-safe: the inventory is opened on the viewer's region thread, and all click,
 * drag and close callbacks already run on that thread (they are driven by Bukkit events on
 * the region that owns the viewer).</p>
 */
public abstract class Menu implements InventoryHolder {

    protected static final MiniMessage MM = MiniMessage.miniMessage();

    protected final SEssentialsPlugin plugin;
    protected final Player viewer;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
    private final Set<Integer> editableSlots = new HashSet<>();

    protected Menu(SEssentialsPlugin plugin, Player viewer, Component title, int rows) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, Math.max(1, rows) * 9, title);
    }

    /** Populates the inventory. Called by {@link #open()} and by in-place re-renders. */
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

    /** Alias of {@link #fill(ItemStack)}: fills every currently-empty slot with {@code filler}. */
    protected void fillEmpty(ItemStack filler) {
        fill(filler);
    }

    /**
     * Fills the outer ring (top and bottom rows, left and right columns) of the chest with
     * {@code item}, skipping any slot that is already occupied.
     *
     * @param item the border filler
     */
    protected void border(ItemStack item) {
        final int size = inventory.getSize();
        final int rows = size / 9;
        for (int i = 0; i < size; i++) {
            final int row = i / 9;
            final int col = i % 9;
            final boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
            if (edge && inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
    }

    /** Marks a single top-inventory slot as editable (freely movable by the player). */
    protected void editable(int slot) {
        editableSlots.add(slot);
    }

    /** Marks the inclusive slot range {@code [from, to]} as editable. */
    protected void editable(int from, int to) {
        final int lo = Math.min(from, to);
        final int hi = Math.max(from, to);
        for (int slot = lo; slot <= hi; slot++) {
            editableSlots.add(slot);
        }
    }

    /**
     * Clears all items and registered handlers (editable-slot markings are structural and
     * preserved). Used by re-rendering menus that rebuild their contents in place.
     */
    protected void clear() {
        inventory.clear();
        handlers.clear();
    }

    /**
     * Handles a raw click. Clicks on editable top slots are left untouched so the player can
     * move items; clicks on locked top slots are cancelled and routed to the slot handler.
     * Clicks inside the player's own inventory are inert on fully-static menus, and are only
     * blocked for shift-moves (which auto-target the top) when the top is not entirely editable.
     */
    public void handleClick(InventoryClickEvent event) {
        final int rawSlot = event.getRawSlot();
        final boolean topSlot = rawSlot >= 0 && rawSlot < inventory.getSize();

        if (topSlot && editableSlots.contains(rawSlot)) {
            // Editable top slot: allow the move, do not route.
            return;
        }

        if (!topSlot) {
            // Player's own inventory (or outside the view).
            if (editableSlots.isEmpty()) {
                // Fully static menu: keep the whole view locked, as the classic base did.
                event.setCancelled(true);
            } else if (event.isShiftClick() && editableSlots.size() != inventory.getSize()) {
                // Shift-move auto-targets the first free top slot, which may be locked; block it
                // unless every top slot is editable (then wherever it lands is fine).
                event.setCancelled(true);
            }
            return;
        }

        // Locked top slot: cancel and dispatch to the registered handler.
        event.setCancelled(true);
        final Consumer<InventoryClickEvent> handler = handlers.get(rawSlot);
        if (handler != null) {
            Sounds.click(viewer);
            handler.accept(event);
        }
    }

    /**
     * Handles a drag. Cancels the drag when any dragged raw slot is a locked (non-editable)
     * top-inventory slot; otherwise the drag is allowed.
     */
    public void handleDrag(InventoryDragEvent event) {
        for (final int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize() && !editableSlots.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Dispatches a close event to the {@link #onClose(InventoryCloseEvent)} hook. */
    public void handleClose(InventoryCloseEvent event) {
        onClose(event);
    }

    /**
     * Called when the menu is closed. No-op by default; override to persist editable contents
     * (backpack/shulker style) or clean up. Runs on the viewer's region thread.
     *
     * @param event the close event
     */
    protected void onClose(InventoryCloseEvent event) {
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
