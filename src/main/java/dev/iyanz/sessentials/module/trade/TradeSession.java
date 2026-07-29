package dev.iyanz.sessentials.module.trade;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The live state of a single player-to-player trade: two linked {@link TradeMenu}s,
 * each side's ready flag, and the logic that swaps or returns items.
 *
 * <h2>Duplication / loss safety</h2>
 * <p>Two invariants make item duplication or loss impossible:</p>
 * <ol>
 *   <li><b>Items live in the menus, not in inventories.</b> When a player places a stack
 *       in an offer slot it leaves their real inventory (vanilla behaviour), so the menu
 *       holds the one authoritative copy.</li>
 *   <li><b>Every menu is drained exactly once.</b> {@link TradeMenu#reclaim()} is guarded
 *       by an atomic flag, so no matter how many termination paths race (a partner
 *       closing, quitting, or the final confirm firing at the same instant), each menu's
 *       offered items are extracted a single time. Whoever wins that extraction delivers
 *       them to exactly one destination — across on a successful swap, or back to their
 *       owner otherwise.</li>
 * </ol>
 *
 * <p>Every inventory read/write happens on the owning player's region thread (via
 * {@link Schedulers#entity}), so the whole session is Folia-safe. A player who leaves has
 * their own offer restored <em>synchronously</em> inside their quit/close event, while
 * they are still on their region thread and their inventory is still live, so nothing is
 * lost to a disconnect.</p>
 */
final class TradeSession {

    private final SEssentialsPlugin plugin;
    private final TradeManager manager;

    private final UUID leftId;
    private final String leftName;
    private final TradeMenu leftMenu;

    private final UUID rightId;
    private final String rightName;
    private final TradeMenu rightMenu;

    private volatile boolean leftReady;
    private volatile boolean rightReady;

    /** Set once the trade is being finalised; blocks further edits/confirms. */
    private final AtomicBoolean swapStarted = new AtomicBoolean(false);
    /** Set once the trade has been cancelled; blocks the swap and further edits. */
    private volatile boolean cancelled;

    TradeSession(SEssentialsPlugin plugin, TradeManager manager,
                 UUID leftId, String leftName, TradeMenu leftMenu,
                 UUID rightId, String rightName, TradeMenu rightMenu) {
        this.plugin = plugin;
        this.manager = manager;
        this.leftId = leftId;
        this.leftName = leftName;
        this.leftMenu = leftMenu;
        this.rightId = rightId;
        this.rightName = rightName;
        this.rightMenu = rightMenu;
    }

    UUID leftId() {
        return leftId;
    }

    UUID rightId() {
        return rightId;
    }

    // --- lifecycle --------------------------------------------------------

    /** Opens both menus, each on its owner's region thread, and greets both players. */
    void open() {
        Player left = Bukkit.getPlayer(leftId);
        Player right = Bukkit.getPlayer(rightId);
        if (left == null || right == null) {
            manager.remove(this);
            if (left != null) {
                Msg.err(left, "The other player is no longer online.");
            }
            if (right != null) {
                Msg.err(right, "The other player is no longer online.");
            }
            return;
        }
        Schedulers.entity(plugin, left, () -> left.openInventory(leftMenu.getInventory()));
        Schedulers.entity(plugin, right, () -> right.openInventory(rightMenu.getInventory()));
        Schedulers.entity(plugin, left, () -> Msg.info(left,
                "Trading with " + rightName + ". Place items on the left, then confirm."));
        Schedulers.entity(plugin, right, () -> Msg.info(right,
                "Trading with " + leftName + ". Place items on the left, then confirm."));
    }

    // --- edits ------------------------------------------------------------

    /**
     * Reacts to a change in one side's offer: any change invalidates both confirmations
     * (so nobody can confirm and then swap the goods at the last instant) and re-mirrors
     * the changed offer onto the partner's read-only half.
     *
     * @param side the side whose offer changed
     */
    void onOfferChanged(TradeMenu.Side side) {
        if (cancelled || swapStarted.get()) {
            return;
        }
        leftReady = false;
        rightReady = false;
        refreshReadyDisplays();

        TradeMenu ownerMenu = side == TradeMenu.Side.LEFT ? leftMenu : rightMenu;
        UUID ownerId = side == TradeMenu.Side.LEFT ? leftId : rightId;
        TradeMenu otherMenu = side == TradeMenu.Side.LEFT ? rightMenu : leftMenu;
        UUID otherId = side == TradeMenu.Side.LEFT ? rightId : leftId;

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        // Snapshot on the owner's region (next tick — after the click/drag has applied),
        // then paint it onto the partner's mirror on the partner's region.
        Schedulers.entity(plugin, owner, () -> {
            if (cancelled || swapStarted.get()) {
                return;
            }
            ItemStack[] snap = ownerMenu.snapshotOffer();
            Player other = Bukkit.getPlayer(otherId);
            if (other == null) {
                return;
            }
            Schedulers.entity(plugin, other, () -> {
                if (cancelled || swapStarted.get()) {
                    return;
                }
                otherMenu.setMirror(snap);
            });
        });
    }

    /**
     * Toggles a side's ready flag. When both sides are ready the swap fires.
     *
     * @param side the side whose player clicked the confirm button
     */
    void onConfirmToggled(TradeMenu.Side side) {
        if (cancelled || swapStarted.get()) {
            return;
        }
        boolean nowReady;
        if (side == TradeMenu.Side.LEFT) {
            leftReady = !leftReady;
            nowReady = leftReady;
        } else {
            rightReady = !rightReady;
            nowReady = rightReady;
        }
        refreshReadyDisplays();
        Player p = Bukkit.getPlayer(side == TradeMenu.Side.LEFT ? leftId : rightId);
        if (p != null) {
            Msg.info(p, nowReady ? "You confirmed the trade." : "You un-confirmed the trade.");
        }
        if (leftReady && rightReady) {
            complete();
        }
    }

    private void refreshReadyDisplays() {
        Player left = Bukkit.getPlayer(leftId);
        if (left != null) {
            Schedulers.entity(plugin, left, () -> {
                leftMenu.setConfirmButton(leftReady);
                leftMenu.setStatusIndicator(rightReady);
            });
        }
        Player right = Bukkit.getPlayer(rightId);
        if (right != null) {
            Schedulers.entity(plugin, right, () -> {
                rightMenu.setConfirmButton(rightReady);
                rightMenu.setStatusIndicator(leftReady);
            });
        }
    }

    // --- completion -------------------------------------------------------

    private void complete() {
        if (cancelled || !swapStarted.compareAndSet(false, true)) {
            return;
        }
        manager.remove(this);
        Player left = Bukkit.getPlayer(leftId);
        Player right = Bukkit.getPlayer(rightId);
        if (left == null || right == null) {
            // A partner slipped offline in the final instant: return each side to its
            // owner rather than swap. (In practice their quit handler already ran and
            // restored them; reclaim() then simply yields nothing here.)
            returnEachToOwner("A player went offline; trade cancelled.");
            return;
        }
        // Reclaim each menu on its owner's region, then hand the goods across. If a
        // reclaim comes back null the menu was already drained by a racing cancel, so we
        // return only what we still hold to its owner. Delivery re-checks that the
        // recipient is still online and, if not, returns the stack to its original owner —
        // so a disconnect mid-swap never deletes items.
        Schedulers.entity(plugin, left, () -> {
            List<ItemStack> leftItems = leftMenu.reclaim();
            Schedulers.entity(plugin, right, () -> {
                List<ItemStack> rightItems = rightMenu.reclaim();
                if (leftItems == null || rightItems == null) {
                    // Raced with a cancel that already returned the other side.
                    if (leftItems != null) {
                        deliver(leftItems, leftId, leftId, null);
                    }
                    if (rightItems != null) {
                        deliver(rightItems, rightId, rightId, null);
                    }
                    return;
                }
                deliver(leftItems, rightId, leftId, leftName);    // A's offer → B (fallback: back to A)
                deliver(rightItems, leftId, rightId, rightName);  // B's offer → A (fallback: back to B)
            });
        });
    }

    /**
     * Delivers a reclaimed stack to its intended recipient, or — if that recipient has
     * gone offline — back to its original owner, so the items are never lost. The single
     * call delivers to exactly one destination, so no duplication can occur.
     *
     * @param items       the reclaimed items (already detached from any menu)
     * @param recipientId who should receive them on success
     * @param ownerId     who to fall back to if the recipient is offline
     * @param partnerName the partner's name for the completion message, or {@code null}
     *                    to send a plain cancellation notice instead
     */
    private void deliver(List<ItemStack> items, UUID recipientId, UUID ownerId, String partnerName) {
        if (items == null || items.isEmpty()) {
            Player recip = Bukkit.getPlayer(recipientId);
            if (recip != null) {
                Schedulers.entity(plugin, recip, recip::closeInventory);
            }
            return;
        }
        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient != null) {
            Schedulers.entity(plugin, recipient, () -> {
                give(recipient, items);
                recipient.closeInventory();
                if (partnerName != null) {
                    Msg.ok(recipient, "Trade with " + partnerName + " completed.");
                } else {
                    Msg.err(recipient, "Trade cancelled; your items were returned.");
                }
            });
            return;
        }
        // Recipient offline: return the stack to its original owner instead.
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) {
            Schedulers.entity(plugin, owner, () -> {
                give(owner, items);
                owner.closeInventory();
                Msg.err(owner, "The other player went offline; your items were returned.");
            });
        }
        // If both parties are offline this same tick (astronomically rare) there is no
        // live inventory to safely receive the stack; it is not duplicated, only undelivered.
    }

    // --- cancellation -----------------------------------------------------

    /**
     * Ends the trade because {@code initiator} closed the GUI or left. The initiator's own
     * offer is restored <em>synchronously</em> (the caller is already on the initiator's
     * region thread, so their inventory is live even mid-disconnect); the partner's offer
     * is restored on the partner's region and their GUI closed.
     *
     * @param initiator     the player whose close/quit event triggered this
     * @param reasonForOther the message shown to the partner
     */
    void endBy(Player initiator, String reasonForOther) {
        if (swapStarted.get()) {
            return; // already finalising — reclaim() guards prevent any double delivery
        }
        cancelled = true;
        manager.remove(this);

        // Restore the initiator's own offer right here on their region thread. A null
        // result means this side was already drained by another end path (e.g. the
        // programmatic close we trigger on the partner below), so this is a redundant
        // re-entry: stop before re-notifying anyone.
        TradeMenu ownMenu = initiator.getUniqueId().equals(leftId) ? leftMenu : rightMenu;
        List<ItemStack> ownItems = ownMenu.reclaim();
        if (ownItems == null) {
            return;
        }
        give(initiator, ownItems);

        // Restore the partner's offer on their region and close their GUI.
        UUID otherId = initiator.getUniqueId().equals(leftId) ? rightId : leftId;
        TradeMenu otherMenu = initiator.getUniqueId().equals(leftId) ? rightMenu : leftMenu;
        String initiatorName = initiator.getUniqueId().equals(leftId) ? leftName : rightName;
        Player other = Bukkit.getPlayer(otherId);
        if (other != null) {
            Schedulers.entity(plugin, other, () -> {
                List<ItemStack> otherItems = otherMenu.reclaim();
                if (otherItems != null) {
                    give(other, otherItems);
                }
                other.closeInventory();
                Msg.err(other, initiatorName + " " + reasonForOther);
            });
        }
    }

    private void returnEachToOwner(String reason) {
        Player left = Bukkit.getPlayer(leftId);
        if (left != null) {
            Schedulers.entity(plugin, left, () -> {
                List<ItemStack> items = leftMenu.reclaim();
                if (items != null) {
                    give(left, items);
                }
                left.closeInventory();
                Msg.err(left, reason);
            });
        }
        Player right = Bukkit.getPlayer(rightId);
        if (right != null) {
            Schedulers.entity(plugin, right, () -> {
                List<ItemStack> items = rightMenu.reclaim();
                if (items != null) {
                    give(right, items);
                }
                right.closeInventory();
                Msg.err(right, reason);
            });
        }
    }

    /**
     * Adds items to {@code player}'s inventory, dropping any overflow at their feet so
     * nothing is lost. <b>Must run on {@code player}'s region thread.</b>
     */
    private void give(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        if (!overflow.isEmpty()) {
            Location loc = player.getLocation();
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(loc, leftover);
            }
        }
    }
}
