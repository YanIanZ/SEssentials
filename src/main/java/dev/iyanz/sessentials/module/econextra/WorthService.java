package dev.iyanz.sessentials.module.econextra;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.Material;

/**
 * Reads configured item sell-worth from {@code config.yml}'s {@code worth} section
 * (e.g. {@code worth.DIAMOND: 10.0} prices one diamond at 10), and renders material
 * names for chat feedback. An item with no {@code worth.<MATERIAL_NAME>} entry has no
 * worth at all (config-driven; there is no built-in default price list).
 */
final class WorthService {

    private WorthService() {
    }

    /**
     * @param plugin   the owning plugin (for its {@code config.yml})
     * @param material the item's material
     * @return the configured unit price, or {@code null} if {@code material} has no
     *         {@code worth.<MATERIAL_NAME>} entry
     */
    static Double unitPrice(SEssentialsPlugin plugin, Material material) {
        String path = "worth." + material.name();
        if (!plugin.getConfig().contains(path)) {
            return null;
        }
        return plugin.getConfig().getDouble(path);
    }

    /**
     * @param material a material such as {@code DIAMOND_SWORD}
     * @return its name rendered as {@code "Diamond Sword"}
     */
    static String displayName(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(material.name().length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
