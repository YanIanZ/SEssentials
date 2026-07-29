package dev.iyanz.sessentials.module.portals;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.gui.ItemBuilder;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Builds and identifies the {@code /portal wand} selection tool: a single
 * {@link Material#GOLDEN_AXE}, named "Portal Wand", tagged with a marker
 * {@link PersistentDataContainer} entry keyed {@code sessentials:portal_wand}.
 */
final class PortalWand {

    private static final String KEY_NAME = "portal_wand";

    private PortalWand() {
    }

    /** @return the {@code sessentials:portal_wand} marker key, scoped to {@code plugin}. */
    private static NamespacedKey key(SEssentialsPlugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    /**
     * Builds a fresh portal wand item.
     *
     * @param plugin the owning plugin (for the PDC key's namespace)
     * @return a single {@link Material#GOLDEN_AXE} tagged as a portal wand
     */
    static ItemStack create(SEssentialsPlugin plugin) {
        ItemStack item = new ItemBuilder(Material.GOLDEN_AXE)
                .name(Style.button(Style.INFO, "Portal Wand"))
                .lore(Style.hint("Left-click: set position 1"), Style.hint("Right-click: set position 2"))
                .glow()
                .build();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * @param plugin the owning plugin (for the PDC key's namespace)
     * @param item   the item to inspect (may be {@code null})
     * @return whether {@code item} is a tagged portal wand
     */
    static boolean isWand(SEssentialsPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(key(plugin), PersistentDataType.BYTE);
    }
}
