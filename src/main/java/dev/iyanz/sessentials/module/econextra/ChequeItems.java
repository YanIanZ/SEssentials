package dev.iyanz.sessentials.module.econextra;

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
 * Builds and reads {@code /cheque} paper items: a single {@link Material#PAPER} item
 * carrying its redeemable amount in a {@link PersistentDataContainer} entry keyed
 * {@code sessentials:cheque_amount} (a {@code double}).
 */
final class ChequeItems {

    private static final String KEY_NAME = "cheque_amount";

    private ChequeItems() {
    }

    /** @return the {@code sessentials:cheque_amount} key, scoped to {@code plugin}. */
    private static NamespacedKey key(SEssentialsPlugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    /**
     * Builds a redeemable cheque item worth {@code amount}.
     *
     * @param plugin    the owning plugin (for the PDC key's namespace)
     * @param amount    the redeemable amount, tagged verbatim onto the item
     * @param formatted the economy-formatted display value (e.g. {@code "$1,500.00"})
     * @return a single {@link Material#PAPER} item tagged with {@code amount}
     */
    static ItemStack create(SEssentialsPlugin plugin, double amount, String formatted) {
        ItemStack item = new ItemBuilder(Material.PAPER)
                .name(Style.button(Style.COIN, "Cheque: " + formatted))
                .lore(Style.hint("Right-click to redeem"))
                .build();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.DOUBLE, amount);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * @param plugin the owning plugin (for the PDC key's namespace)
     * @param item   the item to inspect (may be {@code null})
     * @return the cheque's redeemable amount, or {@code null} if {@code item} is not a
     *         tagged cheque
     */
    static Double readAmount(SEssentialsPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = key(plugin);
        return pdc.has(key, PersistentDataType.DOUBLE) ? pdc.get(key, PersistentDataType.DOUBLE) : null;
    }
}
