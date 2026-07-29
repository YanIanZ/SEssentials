package dev.iyanz.sessentials.module.home;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ConfirmMenu;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Paginated chest GUI listing {@code viewer}'s homes, built on SELIB's {@link PagedMenu}.
 * Each home is one {@link PageEntry}: a themed icon (per-home stored material, else
 * {@link Material#RED_BED}) whose lore shows the world, rounded coordinates and click
 * hints. A left/normal click teleports the viewer there and closes the menu; a shift-click
 * opens a {@link ConfirmMenu} that deletes the home and reopens this list. When the player
 * is under their home limit, a trailing "set a new home" entry closes the menu and points
 * them at {@code /sethome <name>}. Pagination is automatic once the homes overflow the grid.
 *
 * <p>Reuses {@link Homes} for all persistence and the {@link Player#teleportAsync teleportAsync}
 * path for Folia-safe, region-hopping teleports — only the presentation changed.</p>
 *
 * <p>Opened by {@code /homes}; see {@link HomeModule}.</p>
 */
public final class HomesMenu extends PagedMenu {

    private HomesMenu(SEssentialsPlugin plugin, Player viewer, int rows) {
        super(plugin, viewer, MM.deserialize(Style.title("Your Homes")), rows);
    }

    /**
     * Builds and opens the homes menu for {@code viewer}. Sends an informational message
     * instead of an empty menu if they have no homes set.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        List<String> names = new ArrayList<>(Homes.names(plugin, viewer.getUniqueId()));
        if (names.isEmpty()) {
            Msg.info(viewer, "You have no homes set.");
            return;
        }
        new HomesMenu(plugin, viewer, rows(names.size())).open();
    }

    @Override
    protected List<PageEntry> entries() {
        List<String> names = new ArrayList<>(Homes.names(plugin, viewer.getUniqueId()));
        names.sort(String.CASE_INSENSITIVE_ORDER);

        List<PageEntry> entries = new ArrayList<>(names.size() + 1);
        for (String name : names) {
            Location location = Homes.location(plugin, viewer.getUniqueId(), name);
            entries.add(new PageEntry(icon(name, location), event -> onClick(event, name, location)));
        }
        // "Set a new home" affordance, only while the player is under their home limit.
        if (names.size() < Homes.maxHomes(plugin, viewer)) {
            entries.add(new PageEntry(addIcon(), event -> {
                viewer.closeInventory();
                Msg.info(viewer, "Run /sethome <name> to save a new home here.");
            }));
        }
        return entries;
    }

    /** Routes a home click: shift-click deletes (with confirmation), any other click teleports. */
    private void onClick(InventoryClickEvent event, String name, Location location) {
        if (event.isShiftClick()) {
            confirmDelete(name);
        } else {
            teleport(name, location);
        }
    }

    /** Builds the bed icon for home {@code name}, its lore describing {@code location}. */
    private ItemStack icon(String name, Location location) {
        return new ItemBuilder(Homes.icon(plugin, viewer.getUniqueId(), name))
                .name(Style.button(Style.INFO, name))
                .lore(
                        Style.lore(describe(location)),
                        Style.hint("Click to teleport"),
                        Style.BAD + SmallCaps.of("Shift-click to delete"))
                .clean()
                .build();
    }

    /** The trailing "set a new home" prompt shown while under the home limit. */
    private ItemStack addIcon() {
        return new ItemBuilder(Material.LIME_DYE)
                .name(Style.button(Style.DEPOSIT, "Set a New Home"))
                .lore(
                        Style.lore("You can set more homes"),
                        Style.hint("Click, then run /sethome <name>"))
                .clean()
                .build();
    }

    /** Teleports the viewer to {@code location} (re-fetched at build time) and closes the menu. */
    private void teleport(String name, Location location) {
        if (location == null) {
            Msg.err(viewer, "You have no home named \"" + name + "\".");
            return;
        }
        viewer.closeInventory();
        viewer.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(viewer, "Teleported to home \"" + name + "\".");
            } else {
                Msg.err(viewer, "Teleport to home \"" + name + "\" failed.");
            }
        });
    }

    /** Opens a confirmation dialog that deletes home {@code name} and reopens this menu. */
    private void confirmDelete(String name) {
        ItemStack prompt = new ItemBuilder(Homes.icon(plugin, viewer.getUniqueId(), name))
                .name(Style.button(Style.DANGER, "Delete " + name))
                .lore(Style.hint("This cannot be undone"))
                .clean()
                .build();
        new ConfirmMenu(plugin, viewer,
                MM.deserialize(Style.title("Delete Home?")),
                prompt,
                () -> {
                    Homes.delete(plugin, viewer.getUniqueId(), name);
                    Msg.ok(viewer, "Home \"" + name + "\" deleted.");
                    open(plugin, viewer);
                },
                () -> open(plugin, viewer)).open();
    }

    /** @return a human-readable {@code "world (x, y, z)"} description, or a fallback if unresolved. */
    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Location unavailable";
        }
        return location.getWorld().getName() + " (" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /** @return a row count (2-6) sized to fit {@code homeCount} homes plus the add-home entry. */
    private static int rows(int homeCount) {
        int contentRows = Math.max(1, (int) Math.ceil((homeCount + 1) / 9.0));
        return Math.min(6, contentRows + 1);
    }
}
