package dev.iyanz.sessentials.module.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.iyanz.sessentials.gui.ItemBuilder;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * One player's view of a two-sided trade. Each participant gets their <em>own</em>
 * {@code TradeMenu} instance (a 54-slot custom {@link InventoryHolder}); the two menus
 * are linked through a shared {@link TradeSession}. Giving each player a private
 * inventory — rather than opening one shared {@link Inventory} to both — is what keeps
 * the feature Folia-safe: a menu's backing array is only ever touched on its owner's
 * region thread, so two players in different regions never mutate the same object
 * concurrently.
 *
 * <p>Layout (6 rows &times; 9 columns):</p>
 * <ul>
 *   <li>Columns 0-3, rows 0-4 — <b>your offer</b> (the only editable slots).</li>
 *   <li>Column 4, rows 0-4 — an inert glass divider.</li>
 *   <li>Columns 5-8, rows 0-4 — a read-only <b>mirror</b> of the other player's offer.</li>
 *   <li>Slot {@value #CONFIRM_SLOT} — your confirm / ready toggle button.</li>
 *   <li>Slot {@value #STATUS_SLOT} — a read-only indicator of the other player's ready state.</li>
 *   <li>Remaining bottom-row slots — inert labelled glass.</li>
 * </ul>
 *
 * <p><b>Duplication safety:</b> when a player drags an item into an offer slot it
 * physically leaves their real inventory (vanilla container behaviour), so the menu
 * holds the single authoritative copy. {@link #reclaim()} extracts those items exactly
 * once (guarded by an atomic flag) and clears the slots, guaranteeing every offered
 * stack is delivered to exactly one destination — either back to its owner or across to
 * the trade partner — never both, never neither.</p>
 */
public final class TradeMenu implements InventoryHolder {

    /** Total slots in the trade GUI. */
    static final int SIZE = 54;
    /** Slot holding this player's confirm / ready toggle. */
    static final int CONFIRM_SLOT = 49;
    /** Slot holding the read-only indicator of the other player's ready state. */
    static final int STATUS_SLOT = 53;

    /** Which side of a {@link TradeSession} a menu belongs to. */
    public enum Side {
        /** The first participant (the one who accepted, i.e. ran {@code /tradeaccept}). */
        LEFT,
        /** The second participant (the original requester). */
        RIGHT
    }

    private static final int DIVIDER_COL = 4;
    private static final int OFFER_MAX_ROW = 4;
    private static final int OFFER_MAX_COL = 3;

    private final Inventory inventory;
    private final UUID ownerId;
    private final String otherName;
    private final AtomicBoolean reclaimed = new AtomicBoolean(false);

    private TradeSession session;
    private Side side;

    /**
     * Builds (but does not open) a trade menu for one participant.
     *
     * @param ownerId   the UUID of the player this menu belongs to
     * @param ownerName the owner's name (used only for the window title context)
     * @param otherName the trade partner's name (shown on labels and the status indicator)
     */
    TradeMenu(UUID ownerId, String ownerName, String otherName) {
        this.ownerId = ownerId;
        this.otherName = otherName;
        this.inventory = Bukkit.createInventory(this, SIZE,
                MiniMessage.miniMessage().deserialize(Style.title("Trade: " + ownerName + " ⇄ " + otherName)));
        decorate();
    }

    /**
     * Links this menu to its session and side. Called once, immediately after both menus
     * and the session have been constructed.
     *
     * @param session the shared trade session
     * @param side    which side of the session this menu is
     */
    void bind(TradeSession session, Side side) {
        this.session = session;
        this.side = side;
    }

    /** @return the session this menu belongs to. */
    TradeSession session() {
        return session;
    }

    /** @return which side of the session this menu is. */
    Side side() {
        return side;
    }

    /** @return the UUID of the player who owns this menu. */
    UUID ownerId() {
        return ownerId;
    }

    // --- slot classification ----------------------------------------------

    /** @return {@code true} if {@code slot} is one of this player's editable offer slots. */
    static boolean isOfferSlot(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return false;
        }
        int row = slot / 9;
        int col = slot % 9;
        return row <= OFFER_MAX_ROW && col <= OFFER_MAX_COL;
    }

    /** @return the mirror slot on the far side that corresponds to a given offer slot. */
    private static int mirrorSlotOf(int offerSlot) {
        int row = offerSlot / 9;
        int col = offerSlot % 9;
        return row * 9 + (col + 5);
    }

    // --- item mechanics (all called on the OWNER's region thread) ---------

    /**
     * Extracts every offered item exactly once, clearing the offer slots. The atomic
     * guard means that however many code paths race to end the trade (early close,
     * quit, both-confirm), the items leave this menu a single time — the caller then
     * owns the sole copy and is responsible for delivering it.
     *
     * @return the offered items (clones already detached from the slots), or {@code null}
     *         if this menu was already reclaimed by another path
     */
    List<ItemStack> reclaim() {
        if (!reclaimed.compareAndSet(false, true)) {
            return null;
        }
        List<ItemStack> items = new ArrayList<>();
        for (int row = 0; row <= OFFER_MAX_ROW; row++) {
            for (int col = 0; col <= OFFER_MAX_COL; col++) {
                int slot = row * 9 + col;
                ItemStack it = inventory.getItem(slot);
                if (it != null && it.getType() != Material.AIR) {
                    items.add(it.clone());
                }
                inventory.setItem(slot, null);
            }
        }
        return items;
    }

    /**
     * Snapshots the current offer (clones, does not clear) indexed by absolute slot, so
     * the far menu can mirror it into its read-only half without cross-region access.
     *
     * @return a length-{@value #SIZE} array whose offer slots hold clones and whose
     *         other slots are {@code null}
     */
    ItemStack[] snapshotOffer() {
        ItemStack[] snap = new ItemStack[SIZE];
        for (int row = 0; row <= OFFER_MAX_ROW; row++) {
            for (int col = 0; col <= OFFER_MAX_COL; col++) {
                int slot = row * 9 + col;
                ItemStack it = inventory.getItem(slot);
                snap[slot] = (it == null || it.getType() == Material.AIR) ? null : it.clone();
            }
        }
        return snap;
    }

    /**
     * Paints the other player's offer into this menu's read-only mirror half.
     *
     * @param otherOfferBySlot the far menu's {@link #snapshotOffer()} output
     */
    void setMirror(ItemStack[] otherOfferBySlot) {
        for (int row = 0; row <= OFFER_MAX_ROW; row++) {
            for (int col = 0; col <= OFFER_MAX_COL; col++) {
                int offerSlot = row * 9 + col;
                ItemStack it = offerSlot < otherOfferBySlot.length ? otherOfferBySlot[offerSlot] : null;
                inventory.setItem(mirrorSlotOf(offerSlot), it);
            }
        }
    }

    // --- control widgets --------------------------------------------------

    /**
     * Updates the confirm button to reflect this player's ready state.
     *
     * @param ready whether this player has confirmed
     */
    void setConfirmButton(boolean ready) {
        if (ready) {
            inventory.setItem(CONFIRM_SLOT, new ItemBuilder(Material.LIME_WOOL)
                    .name(Style.button(Style.DEPOSIT, "Confirmed"))
                    .lore(Style.lore("Waiting for the other player"),
                            Style.lore("Click again to un-confirm"))
                    .clean().glow().build());
        } else {
            inventory.setItem(CONFIRM_SLOT, new ItemBuilder(Material.RED_WOOL)
                    .name(Style.button(Style.DANGER, "Click to confirm"))
                    .lore(Style.lore("Both players must confirm to trade"),
                            Style.lore("Editing your offer resets this"))
                    .clean().build());
        }
    }

    /**
     * Updates the read-only indicator showing the other player's ready state.
     *
     * @param otherReady whether the trade partner has confirmed
     */
    void setStatusIndicator(boolean otherReady) {
        if (otherReady) {
            inventory.setItem(STATUS_SLOT, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .name(Style.button(Style.DEPOSIT, otherName + " confirmed"))
                    .clean().build());
        } else {
            inventory.setItem(STATUS_SLOT, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .name(Style.button(Style.DANGER, otherName + " not ready"))
                    .clean().build());
        }
    }

    private void decorate() {
        ItemStack dark = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").clean().build();
        ItemStack divider = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").clean().build();
        for (int row = 0; row <= OFFER_MAX_ROW; row++) {
            inventory.setItem(row * 9 + DIVIDER_COL, divider);
        }
        for (int slot : new int[]{46, 47, 48, 50, 51}) {
            inventory.setItem(slot, dark);
        }
        inventory.setItem(45, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name(Style.button(Style.DEPOSIT, "Your offer"))
                .lore(Style.lore("Place items in the left columns"))
                .clean().build());
        inventory.setItem(52, new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .name(Style.button(Style.INFO, otherName + "'s offer"))
                .lore(Style.lore("Read-only preview"))
                .clean().build());
        setConfirmButton(false);
        setStatusIndicator(false);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
