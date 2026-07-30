package dev.iyanz.sessentials.module.guiextras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Sounds;
import dev.iyanz.sessentials.util.Style;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * SELIB {@link PagedMenu} opened by {@code /enchantgui}: a paginated grid of every registered
 * enchantment. Clicking an enchantment cycles its level on the item in the viewer's main hand —
 * {@code none -> 1 -> 2 -> ... -> max -> none} — using "unsafe" application (vanilla level and
 * item-target limits are bypassed, matching the plugin's {@code /enchant} command). Each icon's
 * lore shows the enchantment's max level and its current level on the held item, and the icon
 * glows while the enchantment is applied.
 *
 * <p>The main hand must hold an item: the menu refuses to open on an empty hand, and each click
 * re-checks (the item can change while the menu is open) and errors if the hand is empty.</p>
 *
 * <p>Folia-safe: the menu opens on the viewer's region thread and every click callback already runs
 * there, so the held item is read and mutated on the region that owns the viewer.</p>
 */
final class EnchantMenu extends PagedMenu {

    private static final int ROWS = 6;

    /** The non-deprecated enchantment registry (same access path as the items module). */
    private static final Registry<Enchantment> ENCHANTMENTS =
            RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

    private EnchantMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Enchantments")), ROWS);
    }

    /**
     * Opens the enchantment grid for {@code viewer}, or errors if the main hand is empty.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    static void open(SEssentialsPlugin plugin, Player viewer) {
        if (viewer.getInventory().getItemInMainHand().getType().isAir()) {
            Msg.err(viewer, "You must hold an item to enchant.");
            Sounds.fail(viewer);
            return;
        }
        new EnchantMenu(plugin, viewer).open();
    }

    @Override
    protected List<PageEntry> entries() {
        final List<Enchantment> sorted = new ArrayList<>();
        for (final Enchantment enchantment : ENCHANTMENTS) {
            sorted.add(enchantment);
        }
        sorted.sort(Comparator.comparing(e -> e.getKey().getKey()));

        final List<PageEntry> out = new ArrayList<>(sorted.size());
        for (final Enchantment enchantment : sorted) {
            out.add(new PageEntry(icon(enchantment), event -> cycle(enchantment)));
        }
        return out;
    }

    /** Builds the book icon for one enchantment, showing its max and current level. */
    private ItemStack icon(Enchantment enchantment) {
        final int max = enchantment.getMaxLevel();
        final int current = currentLevel(enchantment);

        final List<String> lore = new ArrayList<>();
        lore.add(Style.labelled("Max Level", String.valueOf(max)));
        lore.add(current > 0
                ? Style.labelled("Current", String.valueOf(current))
                : Style.lore("Not applied"));
        lore.add(Style.hint(current >= max ? "Click to remove" : "Click to increase"));

        final ItemBuilder builder = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name(Style.button(current > 0 ? Style.OK : Style.INFO, pretty(enchantment)))
                .lore(lore)
                .clean();
        if (current > 0) {
            builder.glow();
        }
        return builder.build();
    }

    /**
     * Cycles {@code enchantment} one level on the held item: absent&rarr;1, then +1 up to the
     * enchantment's max, then back to removed. Errors if the hand is empty. Runs on the viewer's
     * region thread (the click handler already does), then re-renders.
     */
    private void cycle(Enchantment enchantment) {
        final ItemStack hand = viewer.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(viewer, "You are not holding an item.");
            Sounds.fail(viewer);
            return;
        }

        final int max = enchantment.getMaxLevel();
        final ItemMeta meta = hand.getItemMeta();
        final int current = meta.getEnchantLevel(enchantment);
        final int next = current >= max ? 0 : current + 1;
        final String name = pretty(enchantment);

        if (next == 0) {
            meta.removeEnchant(enchantment);
            hand.setItemMeta(meta);
            viewer.getInventory().setItemInMainHand(hand);
            Msg.info(viewer, "Removed " + name + ".");
        } else {
            meta.addEnchant(enchantment, next, true);
            hand.setItemMeta(meta);
            viewer.getInventory().setItemInMainHand(hand);
            Msg.ok(viewer, "Applied " + name + " " + next + ".");
            Sounds.upgrade(viewer);
        }
        refresh();
    }

    /** @return the level of {@code enchantment} on the held item, or {@code 0} if absent/empty-handed. */
    private int currentLevel(Enchantment enchantment) {
        final ItemStack hand = viewer.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return 0;
        }
        final ItemMeta meta = hand.getItemMeta();
        return meta == null ? 0 : meta.getEnchantLevel(enchantment);
    }

    /**
     * Renders an enchantment key ({@code "fire_aspect"}) as a plain title-cased label
     * ({@code "Fire Aspect"}). The small-caps transform is left to {@link Style}/{@link Msg},
     * which apply it once when the label is displayed.
     */
    private static String pretty(Enchantment enchantment) {
        final String[] parts = enchantment.getKey().getKey().toLowerCase(Locale.ROOT).split("_");
        final StringBuilder out = new StringBuilder();
        for (final String part : parts) {
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
