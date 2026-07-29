package dev.iyanz.sessentials.selib.gui;

import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB factory of common, ready-made menu items. Labels use the shared {@link Style}
 * palette and are rendered in {@link SmallCaps} for a consistent look. Every method returns
 * a fresh {@link ItemStack}, so callers may safely mutate the result.
 */
public final class Buttons {

    private Buttons() {
    }

    /** A blank grey pane used to pad empty menu space. */
    public static ItemStack filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** A blank pane of the given material (blank name, clean tooltip). */
    public static ItemStack filler(Material material) {
        return new ItemBuilder(material).name(Component.empty()).clean().build();
    }

    /** A "back" arrow button. */
    public static ItemStack back() {
        return new ItemBuilder(Material.ARROW)
                .name(Style.button(Style.INFO, "Back"))
                .lore(Style.hint("Click to go back"))
                .clean()
                .build();
    }

    /** A "close" button. */
    public static ItemStack close() {
        return new ItemBuilder(Material.BARRIER)
                .name(Style.button(Style.DANGER, "Close"))
                .lore(Style.hint("Click to close"))
                .clean()
                .build();
    }

    /**
     * A "previous page" arrow.
     *
     * @param page the 1-based page it navigates to
     */
    public static ItemStack prev(int page) {
        return new ItemBuilder(Material.ARROW)
                .name(Style.button(Style.INFO, "Previous"))
                .lore(Style.lore("Page " + page))
                .clean()
                .build();
    }

    /**
     * A "next page" arrow.
     *
     * @param page the 1-based page it navigates to
     */
    public static ItemStack next(int page) {
        return new ItemBuilder(Material.ARROW)
                .name(Style.button(Style.INFO, "Next"))
                .lore(Style.lore("Page " + page))
                .clean()
                .build();
    }

    /**
     * A non-clickable page indicator, e.g. {@code "Page 2 / 5"}.
     *
     * @param current the 1-based current page
     * @param total   the total page count
     */
    public static ItemStack pageIndicator(int current, int total) {
        return new ItemBuilder(Material.PAPER)
                .name(Style.INFO + "<bold>" + SmallCaps.of("Page") + " " + current + " / " + total)
                .clean()
                .build();
    }
}
