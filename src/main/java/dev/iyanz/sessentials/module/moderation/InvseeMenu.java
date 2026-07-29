package dev.iyanz.sessentials.module.moderation;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.gui.ItemBuilder;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

/**
 * A live, editable "invsee" GUI showing another player's full inventory — the 36
 * storage slots plus the four armour pieces and the off-hand — so a moderator can
 * inspect and forcibly take or place items. Changes are written back to the target on
 * close.
 *
 * <p>Folia-safe: the target's inventory is snapshotted on the target's region thread
 * before the GUI opens, and written back on the target's region thread on close, so no
 * cross-region inventory access occurs.</p>
 *
 * <p>Layout (54 slots): 0-35 = storage; 45-48 = helmet/chestplate/leggings/boots;
 * 49 = off-hand. The remaining slots are inert labelled glass.</p>
 */
public final class InvseeMenu implements InventoryHolder {

    /** GUI slots that map to real inventory slots and are therefore editable. */
    static final int HELMET = 45;
    static final int CHEST = 46;
    static final int LEGS = 47;
    static final int BOOTS = 48;
    static final int OFFHAND = 49;

    private final SEssentialsPlugin plugin;
    private final UUID targetId;
    private final String targetName;
    private final Inventory inventory;

    private InvseeMenu(SEssentialsPlugin plugin, UUID targetId, String targetName) {
        this.plugin = plugin;
        this.targetId = targetId;
        this.targetName = targetName;
        this.inventory = Bukkit.createInventory(this, 54,
                MiniMessage.miniMessage().deserialize(Style.title(targetName + "'s Inventory")));
    }

    /**
     * Opens the invsee GUI for {@code viewer} onto {@code target}.
     *
     * @param plugin the plugin
     * @param viewer the moderator
     * @param target the inspected player
     */
    public static void open(SEssentialsPlugin plugin, Player viewer, Player target) {
        Schedulers.entity(plugin, target, () -> {
            PlayerInventory inv = target.getInventory();
            ItemStack[] storage = clone(inv.getStorageContents());
            ItemStack helmet = cloneOne(inv.getHelmet());
            ItemStack chest = cloneOne(inv.getChestplate());
            ItemStack legs = cloneOne(inv.getLeggings());
            ItemStack boots = cloneOne(inv.getBoots());
            ItemStack offhand = cloneOne(inv.getItemInOffHand());
            UUID id = target.getUniqueId();
            String name = target.getName();
            Schedulers.entity(plugin, viewer, () -> {
                InvseeMenu menu = new InvseeMenu(plugin, id, name);
                menu.populate(storage, helmet, chest, legs, boots, offhand);
                viewer.openInventory(menu.inventory);
            });
        });
    }

    private void populate(ItemStack[] storage, ItemStack helmet, ItemStack chest,
                          ItemStack legs, ItemStack boots, ItemStack offhand) {
        for (int i = 0; i < 36 && i < storage.length; i++) {
            inventory.setItem(i, storage[i]);
        }
        inventory.setItem(HELMET, helmet);
        inventory.setItem(CHEST, chest);
        inventory.setItem(LEGS, legs);
        inventory.setItem(BOOTS, boots);
        inventory.setItem(OFFHAND, offhand);
        // Inert labels on the rest of the armour row.
        ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").clean().build();
        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(50, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(Style.GRAY + SmallCaps.of("Armour → helmet, chest, legs, boots, off-hand"))
                .clean().build());
        for (int i = 51; i < 54; i++) {
            inventory.setItem(i, filler);
        }
    }

    /** @return {@code true} if {@code guiSlot} maps to a real inventory slot (editable). */
    static boolean isEditable(int guiSlot) {
        return (guiSlot >= 0 && guiSlot < 36) || guiSlot == HELMET || guiSlot == CHEST
                || guiSlot == LEGS || guiSlot == BOOTS || guiSlot == OFFHAND;
    }

    /** Writes the GUI's editable slots back to the (online) target's inventory. */
    void writeBack() {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            return; // target left — discard edits rather than touch an offline profile
        }
        ItemStack[] contents = inventory.getContents();
        Schedulers.entity(plugin, target, () -> {
            PlayerInventory inv = target.getInventory();
            ItemStack[] storage = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                storage[i] = contents[i];
            }
            inv.setStorageContents(storage);
            inv.setHelmet(contents[HELMET]);
            inv.setChestplate(contents[CHEST]);
            inv.setLeggings(contents[LEGS]);
            inv.setBoots(contents[BOOTS]);
            inv.setItemInOffHand(contents[OFFHAND] == null ? new ItemStack(Material.AIR) : contents[OFFHAND]);
        });
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
