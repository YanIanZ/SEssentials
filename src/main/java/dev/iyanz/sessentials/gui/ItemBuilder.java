package dev.iyanz.sessentials.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Small fluent builder for menu {@link ItemStack}s with MiniMessage (hex-capable)
 * display names and lore. Italic decoration is disabled by default so names render
 * as written.
 */
public final class ItemBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /** Sets the display name from a MiniMessage string. */
    public ItemBuilder name(String miniMessage) {
        meta.displayName(MM.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false));
        return this;
    }

    /** Sets the display name from a component. */
    public ItemBuilder name(Component component) {
        meta.displayName(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    /** Sets lore from MiniMessage strings. */
    public ItemBuilder lore(String... miniMessageLines) {
        return lore(Arrays.asList(miniMessageLines));
    }

    /** Sets lore from MiniMessage strings. */
    public ItemBuilder lore(List<String> miniMessageLines) {
        List<Component> lore = new ArrayList<>(miniMessageLines.size());
        for (String line : miniMessageLines) {
            lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        return this;
    }

    /** Sets the stack amount. */
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    /**
     * On a {@code PLAYER_HEAD}, textures the skull with {@code owner}'s skin. No-op
     * for non-skull items or a {@code null} owner.
     *
     * @param owner the player whose skin to show
     */
    public ItemBuilder skull(OfflinePlayer owner) {
        if (owner != null && meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
        }
        return this;
    }

    /** Hides attribute lines, dye/potion info and enchant glint tooltips for a clean look. */
    public ItemBuilder clean() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    /** Adds a subtle enchantment glint (with its tooltip hidden) to highlight the item. */
    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    /** @return the finished item. */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
