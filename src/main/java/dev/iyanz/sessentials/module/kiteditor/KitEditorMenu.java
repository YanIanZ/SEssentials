package dev.iyanz.sessentials.module.kiteditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.module.kits.Kit;
import dev.iyanz.sessentials.module.kits.KitService;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Sounds;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Clean-room, CMI-style editable kit-definition GUI: a 6-row chest whose top five rows are
 * an editable item grid (slots {@code 0..44}) and whose bottom row is a locked control row
 * (Save, an info panel, and Cancel). Pre-loaded with the kit's current items if it exists.
 *
 * <p>This is a <strong>definition</strong> editor, not a "give": the items an admin drags
 * into the grid are a <em>template</em>. On Save, the grid's contents are serialised into
 * the kit store (via {@link KitService#save}, exactly the {@code /kit create} format) and
 * the physical items are consumed from the grid — they are never handed back, so a template
 * can never both persist <em>and</em> return to the player (no item dupe). On Cancel — or on
 * closing the menu without saving — nothing is persisted and the physical items are returned
 * to the admin's inventory (overflow drops at their feet), so cancelling never eats items.
 * The grid is the single source of truth in both directions.</p>
 *
 * <p>Folia-safe: opened on the viewer's region thread by {@link Menu#open()}; the Save/Cancel
 * click handlers and the {@link #onClose} return-items path all run on that same region
 * thread (Bukkit drives them from events on the region owning the viewer), so the store
 * write, the viewer-inventory mutation and the world drop are all correctly threaded.</p>
 */
final class KitEditorMenu extends Menu {

    private static final int ROWS = 6;
    /** Slots {@code 0..LAST_GRID_SLOT} are the editable item grid (top five rows). */
    private static final int LAST_GRID_SLOT = 44;
    private static final int SAVE_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int CANCEL_SLOT = 53;

    private final KitService service;
    private final String kitName;
    private final boolean existed;
    private final int cooldownSeconds;
    /** Clones of the kit's current items, laid into the grid by {@link #build()}. */
    private final List<ItemStack> initialItems;

    /**
     * {@code true} once Save has persisted the grid to the store; tells {@link #onClose} the
     * physical grid items were consumed into the template and must NOT be returned (dupe guard).
     */
    private boolean saved;

    private KitEditorMenu(SEssentialsPlugin plugin, Player viewer, KitService service,
                          String kitName, boolean existed, int cooldownSeconds, List<ItemStack> initialItems) {
        super(plugin, viewer, MM.deserialize(Style.title("Kit Editor: " + kitName)), ROWS);
        this.service = service;
        this.kitName = kitName;
        this.existed = existed;
        this.cooldownSeconds = cooldownSeconds;
        this.initialItems = initialItems;
    }

    /**
     * Builds and opens the kit editor for {@code viewer}, pre-loading the kit's items if it
     * already exists. Refuses to open (with a message) only if an existing kit somehow holds
     * more items than the editable grid can display, so editing could not round-trip them.
     *
     * @param plugin  the owning plugin
     * @param service the kit service (bound to the shared kit stores)
     * @param viewer  the admin opening the editor
     * @param name    the kit name (already lower-cased, as stored)
     */
    static void open(SEssentialsPlugin plugin, KitService service, Player viewer, String name) {
        Optional<Kit> existing = service.get(name);
        boolean existed = existing.isPresent();

        List<ItemStack> items = new ArrayList<>();
        if (existed) {
            for (ItemStack stack : existing.get().items()) {
                if (stack != null) {
                    items.add(stack.clone());
                }
            }
        }
        if (items.size() > LAST_GRID_SLOT + 1) {
            Msg.err(viewer, "Kit " + name + " has more items than the editor grid can hold; not opening.");
            return;
        }

        int cooldown = existing.map(Kit::cooldownSeconds).orElseGet(service::defaultCooldownSeconds);
        new KitEditorMenu(plugin, viewer, service, name, existed, cooldown, items).open();
    }

    @Override
    protected void build() {
        Inventory inv = getInventory();

        // Lay the kit's current items into the editable grid (raw contents, no click handler).
        for (int i = 0; i < initialItems.size() && i <= LAST_GRID_SLOT; i++) {
            inv.setItem(i, initialItems.get(i));
        }

        // Locked control row background.
        ItemStack filler = Buttons.filler();
        for (int slot = LAST_GRID_SLOT + 1; slot < ROWS * 9; slot++) {
            set(slot, filler, null);
        }

        // Control buttons (overwrite the filler beneath them).
        set(SAVE_SLOT, saveButton(), event -> onSave());
        set(INFO_SLOT, infoItem(), null);
        set(CANCEL_SLOT, cancelButton(), event -> onCancel());

        // Only the grid is editable; the whole control row stays locked.
        editable(0, LAST_GRID_SLOT);
    }

    /** Serialises the grid into the kit store (existing cooldown, or default for a new kit) and consumes the items. */
    private void onSave() {
        Inventory inv = getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot <= LAST_GRID_SLOT; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                items.add(stack.clone());
            }
        }

        // Re-read the cooldown at save time so an existing kit keeps its current value.
        int cooldown = service.get(kitName).map(Kit::cooldownSeconds).orElseGet(service::defaultCooldownSeconds);
        service.save(kitName, items, cooldown);

        // Consume the physical grid items: the template is a stored copy, not a give. Clearing
        // the grid AND flagging saved makes it impossible for onClose to also return them.
        for (int slot = 0; slot <= LAST_GRID_SLOT; slot++) {
            inv.setItem(slot, null);
        }
        saved = true;

        Msg.ok(viewer, "Saved kit " + kitName + " with " + items.size() + " item(s).");
        Sounds.coins(viewer);
        viewer.closeInventory();
    }

    /** Discards edits and closes; {@link #onClose} returns the physical grid items to the admin. */
    private void onCancel() {
        Msg.info(viewer, "Kit editor cancelled — no changes saved.");
        viewer.closeInventory();
    }

    /**
     * On close without a Save, returns only the items the admin PERSONALLY added, never the
     * pre-loaded template clones. The grid is seeded with clones of the existing kit's items
     * (loaded from the store, not taken from the admin); returning those on cancel would hand
     * the admin a free physical copy of the kit while the stored definition stays intact — an
     * item-duplication bug. So the return set is the grid contents minus one matching stack per
     * pre-loaded template entry; only the surplus (the admin's own dragged-in items) is given
     * back. After a Save the grid is empty and {@link #saved} is set, so nothing is returned.
     */
    @Override
    protected void onClose(InventoryCloseEvent event) {
        if (saved) {
            return;
        }
        Inventory inv = getInventory();

        // A consumable multiset of the pre-loaded template stacks (never returned).
        List<ItemStack> template = new ArrayList<>();
        for (ItemStack seed : initialItems) {
            if (seed != null && !seed.getType().isAir()) {
                template.add(seed.clone());
            }
        }

        List<ItemStack> back = new ArrayList<>();
        for (int slot = 0; slot <= LAST_GRID_SLOT; slot++) {
            ItemStack stack = inv.getItem(slot);
            inv.setItem(slot, null);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            int surplus = stack.getAmount();
            // Cancel out amounts that belong to the template so they aren't handed back.
            for (ItemStack seed : template) {
                if (surplus <= 0) {
                    break;
                }
                if (seed.getAmount() > 0 && seed.isSimilar(stack)) {
                    int taken = Math.min(surplus, seed.getAmount());
                    seed.setAmount(seed.getAmount() - taken);
                    surplus -= taken;
                }
            }
            if (surplus > 0) {
                ItemStack give = stack.clone();
                give.setAmount(surplus);
                back.add(give);
            }
        }
        if (back.isEmpty()) {
            return;
        }
        var overflow = viewer.getInventory().addItem(back.toArray(new ItemStack[0]));
        for (ItemStack leftover : overflow.values()) {
            viewer.getWorld().dropItemNaturally(viewer.getLocation(), leftover);
        }
    }

    private ItemStack saveButton() {
        return new ItemBuilder(Material.LIME_DYE)
                .name(Style.button(Style.DEPOSIT, "Save Kit"))
                .lore(List.of(
                        Style.lore("Stores the grid above as kit"),
                        Style.lore("\"" + kitName + "\" (cooldown " + KitService.formatDuration(cooldownSeconds) + ")."),
                        "",
                        Style.lore("Saving keeps a copy — the items"),
                        Style.lore("here are the kit template and"),
                        Style.lore("are consumed on save."),
                        "",
                        Style.hint("Click to save")))
                .clean()
                .build();
    }

    private ItemStack cancelButton() {
        return new ItemBuilder(Material.BARRIER)
                .name(Style.button(Style.DANGER, "Cancel"))
                .lore(List.of(
                        Style.lore("Discard changes and return"),
                        Style.lore("your placed items."),
                        "",
                        Style.hint("Click to cancel")))
                .clean()
                .build();
    }

    private ItemStack infoItem() {
        List<String> lore = new ArrayList<>();
        lore.add(Style.labelled("Kit:", kitName));
        lore.add(Style.labelled("Status:", existed ? "editing existing" : "new kit"));
        lore.add(Style.labelled("Cooldown:", KitService.formatDuration(cooldownSeconds)));
        lore.add("");
        lore.add(Style.lore("Drag items into the grid above,"));
        lore.add(Style.lore("then click Save to store them."));
        return new ItemBuilder(Material.WRITABLE_BOOK)
                .name(Style.button(Style.INFO, "Kit Editor"))
                .lore(lore)
                .clean()
                .build();
    }
}
