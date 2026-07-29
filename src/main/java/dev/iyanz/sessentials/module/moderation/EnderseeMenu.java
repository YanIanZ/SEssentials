package dev.iyanz.sessentials.module.moderation;

import java.util.Objects;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A live, editable "endersee" GUI showing another player's ender chest — the 27 storage
 * slots — so a moderator can inspect and forcibly take or place items. Changes are
 * written back to the target on close. This mirrors {@link InvseeMenu} for the ender
 * chest so that {@code /endersee} keeps the same view-and-edit behaviour {@code /invsee}
 * has, without the cross-region live-inventory access the old direct-open path used.
 *
 * <p>Folia-safe: the target's ender chest is snapshotted on the target's region thread
 * before the GUI opens, and written back on the target's region thread on close, so no
 * cross-region inventory access occurs.</p>
 *
 * <p>Layout (27 slots): 0-26 = ender-chest storage, every slot editable.</p>
 */
public final class EnderseeMenu implements InventoryHolder {

    /** Vanilla ender chest size; the GUI mirrors it one-to-one. */
    static final int SIZE = 27;

    private final SEssentialsPlugin plugin;
    private final UUID targetId;
    private final Inventory inventory;
    /**
     * The target's ender chest as it was when the GUI opened, indexed by slot (0-26). On
     * close we diff the live GUI contents against this snapshot and write back ONLY the
     * slots the moderator actually changed, so edits the target made to untouched slots
     * while being viewed are not clobbered.
     */
    private final ItemStack[] openSnapshot = new ItemStack[SIZE];

    private EnderseeMenu(SEssentialsPlugin plugin, UUID targetId, String targetName) {
        this.plugin = plugin;
        this.targetId = targetId;
        this.inventory = Bukkit.createInventory(this, SIZE,
                MiniMessage.miniMessage().deserialize(Style.title(targetName + "'s Ender Chest")));
    }

    /**
     * Opens the endersee GUI for {@code viewer} onto {@code target}.
     *
     * @param plugin the plugin
     * @param viewer the moderator
     * @param target the inspected player
     */
    public static void open(SEssentialsPlugin plugin, Player viewer, Player target) {
        Schedulers.entity(plugin, target, () -> {
            // Read the target's LIVE ender chest on the target's OWN region thread.
            ItemStack[] contents = clone(target.getEnderChest().getContents());
            UUID id = target.getUniqueId();
            String name = target.getName();
            Schedulers.entity(plugin, viewer, () -> {
                EnderseeMenu menu = new EnderseeMenu(plugin, id, name);
                menu.populate(contents);
                viewer.openInventory(menu.inventory);
            });
        });
    }

    private void populate(ItemStack[] contents) {
        for (int i = 0; i < SIZE && i < contents.length; i++) {
            inventory.setItem(i, contents[i]);
            openSnapshot[i] = contents[i];
        }
    }

    /**
     * Applies the moderator's edits back to the (online) target's LIVE ender chest. Only
     * the slots whose GUI content differs from the open-time {@link #openSnapshot} are
     * written; every other slot keeps the target's current live value. This avoids the
     * dupe/delete that a blanket snapshot overwrite would cause when the target changed
     * their own ender chest while it was being viewed. All live-inventory access runs on
     * the target's region thread (Folia-safe).
     */
    void writeBack() {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            return; // target left — discard edits rather than touch an offline profile
        }
        ItemStack[] contents = inventory.getContents();
        Schedulers.entity(plugin, target, () -> {
            // Re-read the LIVE ender chest on the target's own region thread and mutate
            // only the slots the moderator actually changed; untouched slots are left as
            // the target's current live contents.
            Inventory ender = target.getEnderChest();
            for (int i = 0; i < SIZE; i++) {
                if (changed(contents[i], openSnapshot[i])) {
                    ender.setItem(i, contents[i]);
                }
            }
        });
    }

    /** @return {@code true} if the moderator changed this slot (GUI value != open snapshot). */
    private static boolean changed(ItemStack gui, ItemStack snapshot) {
        return !Objects.equals(normalize(gui), normalize(snapshot));
    }

    /** Treats AIR and {@code null} as the same "empty" value for diffing. */
    private static ItemStack normalize(ItemStack item) {
        return (item == null || item.getType() == Material.AIR) ? null : item;
    }

    private static ItemStack[] clone(ItemStack[] src) {
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = cloneOne(src[i]);
        }
        return out;
    }

    private static ItemStack cloneOne(ItemStack src) {
        return (src == null || src.getType() == Material.AIR) ? null : src.clone();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
