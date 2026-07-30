package dev.iyanz.sessentials.module.itembrowser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB paged chest GUI listing every obtainable {@link Material} as a single icon (clean-room,
 * own layout). The material set is {@link Material#values()} filtered to real, non-legacy items
 * ({@link Material#isItem()} and not {@link Material#isLegacy()}, excluding {@link Material#AIR}),
 * sorted alphabetically by material name. The list auto-paginates via {@link PagedMenu}: the content
 * region is filled with icons and the bottom row carries the previous / page-indicator / next
 * controls.
 *
 * <p>Each icon's lore shows the item's namespaced key plus a hint that a left-click gives one and a
 * shift-click gives a full stack. Clicking an icon hands the viewer the item: the items are added to
 * the viewer's inventory and any overflow is dropped naturally at their feet.</p>
 *
 * <p><b>Folia-safety.</b> {@link #entries()} (called on the viewer's region thread by
 * {@link PagedMenu#build()}) only reads static material metadata to build icons, and the click
 * handler — which mutates the viewer's inventory and drops overflow in the viewer's world — is
 * dispatched by the SELIB menu listener on the viewer's own region thread. No cross-region entity
 * state is touched and no {@code Bukkit.getScheduler()} is used.</p>
 */
public final class ItemBrowserMenu extends PagedMenu {

    private static final int ROWS = 6;

    /**
     * Every obtainable material, filtered and sorted once. Immutable and shared across all menus;
     * icons built from it are copied into each inventory by Bukkit, so sharing is safe.
     */
    private static final List<Material> MATERIALS = buildMaterials();

    public ItemBrowserMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Item Browser")), ROWS);
    }

    /** @return the obtainable materials: real, non-legacy items (excluding AIR), sorted by name. */
    private static List<Material> buildMaterials() {
        List<Material> list = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material == Material.AIR || material.isLegacy() || !material.isItem()) {
                continue;
            }
            list.add(material);
        }
        list.sort(Comparator.comparing(Material::name));
        return List.copyOf(list);
    }

    @Override
    protected List<PageEntry> entries() {
        List<PageEntry> list = new ArrayList<>(MATERIALS.size());
        for (Material material : MATERIALS) {
            list.add(new PageEntry(icon(material), event -> give(material, event.isShiftClick())));
        }
        return list;
    }

    /** Builds the display icon for a material: its own item with a branded name and hint lore. */
    private static ItemStack icon(Material material) {
        return new ItemBuilder(material)
                .name(Style.button(Style.BRAND, titleCase(material.name())))
                .lore(
                        Style.labelled("Key:", "minecraft:" + material.name().toLowerCase(Locale.ROOT)),
                        "",
                        Style.hint("Left-click: 1"),
                        Style.hint("Shift-click: full stack"))
                .build();
    }

    /**
     * Hands the viewer the clicked item. Runs on the viewer's region thread (the SELIB menu listener
     * dispatches clicks there), so mutating the viewer's inventory and dropping in their world are
     * both Folia-safe.
     *
     * @param material the clicked material
     * @param shift    {@code true} for a full stack, {@code false} for a single item
     */
    private void give(Material material, boolean shift) {
        int amount = shift ? material.getMaxStackSize() : 1;
        Map<Integer, ItemStack> overflow = viewer.getInventory().addItem(new ItemStack(material, amount));
        for (ItemStack leftover : overflow.values()) {
            viewer.getWorld().dropItemNaturally(viewer.getLocation(), leftover);
        }
    }

    /** Renders a {@code SNAKE_CASE} material name as human-readable {@code "Title Case"}. */
    private static String titleCase(String snakeCase) {
        String[] parts = snakeCase.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(snakeCase.length());
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
