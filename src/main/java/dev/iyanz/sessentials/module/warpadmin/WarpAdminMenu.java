package dev.iyanz.sessentials.module.warpadmin;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ConfirmMenu;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Clean-room, CMI-style <em>warp admin</em> GUI, built on SELIB's {@link PagedMenu}. Lists every
 * warp read from the shared warp store (see {@link AdminWarps}) as a paginated grid; each entry is
 * a themed icon (the stored warp material, else {@link Material#ENDER_PEARL}) whose lore shows the
 * world, rounded coordinates and the available actions.
 *
 * <p><strong>Reuse-only front-end.</strong> The store is read read-only; every mutation is
 * delegated to an existing, already-audited command:</p>
 * <ul>
 *   <li><b>Left-click</b> a warp → teleport there via {@code viewer.performCommand("warp <name>")}
 *       (reusing the warp command's audited teleport) and close.</li>
 *   <li><b>Shift- or right-click</b> a warp → a {@link ConfirmMenu}; confirming dispatches
 *       {@code delwarp <name>} as the console, then refreshes the list.</li>
 *   <li>A bottom-row <b>"Create warp at my location"</b> button → closes and tells the player to
 *       run {@code /setwarp <name>} (a name argument can't be safely collected here).</li>
 * </ul>
 *
 * <p>Folia-safe: the menu is opened and its clicks routed on the viewer's region thread;
 * {@code performCommand} runs on that same thread; the console {@code delwarp} dispatch runs on the
 * global region scheduler (matching the rest of the suite) and the refresh hops back to the
 * viewer's region thread only after the delete has been applied.</p>
 *
 * <p>Opened by {@code /warpadmin}; see {@link WarpAdminModule}.</p>
 */
public final class WarpAdminMenu extends PagedMenu {

    /** Bottom-row slot for the "create warp" button (between the prev button and the page indicator). */
    private static final int CREATE_OFFSET = 2;
    /** Bottom-row slot for the close button (between the page indicator and the next button). */
    private static final int CLOSE_OFFSET = 6;

    private WarpAdminMenu(SEssentialsPlugin plugin, Player viewer, int rows) {
        super(plugin, viewer, MM.deserialize(Style.title("Warp Admin")), rows);
    }

    /**
     * Builds and opens the warp-admin menu for {@code viewer}. Always opens, even with no warps,
     * so the "create warp" button remains reachable.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        new WarpAdminMenu(plugin, viewer, rows(AdminWarps.names(plugin).size())).open();
    }

    @Override
    protected void build() {
        // PagedMenu lays out the content grid and the bottom navigation row (prev / indicator /
        // next, padded with filler). Add the admin controls into the two free navigation-row gaps.
        super.build();
        final int bottom = getInventory().getSize() - 9;
        set(bottom + CREATE_OFFSET, createButton(), event -> promptCreate());
        set(bottom + CLOSE_OFFSET, Buttons.close(), event -> viewer.closeInventory());
    }

    @Override
    protected List<PageEntry> entries() {
        List<String> names = new ArrayList<>(AdminWarps.names(plugin));
        names.sort(String.CASE_INSENSITIVE_ORDER);

        List<PageEntry> entries = new ArrayList<>(names.size());
        for (String name : names) {
            Location location = AdminWarps.location(plugin, name);
            entries.add(new PageEntry(icon(name, location), event -> onClick(event, name)));
        }
        return entries;
    }

    /** Routes a warp click: shift- or right-click deletes (with confirmation), any other click warps. */
    private void onClick(InventoryClickEvent event, String name) {
        if (event.isShiftClick() || event.isRightClick()) {
            confirmDelete(name);
        } else {
            teleport(name);
        }
    }

    /** Closes the menu and teleports the viewer via the audited {@code /warp} command. */
    private void teleport(String name) {
        viewer.closeInventory();
        viewer.performCommand("warp " + name);
    }

    /**
     * Opens a confirmation dialog. Confirming deletes the warp through the audited {@code /delwarp}
     * command dispatched as the console (on the global region scheduler, Folia-safe), then reopens
     * this menu — only after the delete has been applied, so the refreshed list is accurate.
     * Cancelling simply reopens the list.
     */
    private void confirmDelete(String name) {
        ItemStack prompt = new ItemBuilder(AdminWarps.icon(plugin, name))
                .name(Style.button(Style.DANGER, "Delete " + name))
                .lore(Style.hint("This cannot be undone"))
                .clean()
                .build();
        new ConfirmMenu(plugin, viewer,
                MM.deserialize(Style.title("Delete Warp?")),
                prompt,
                () -> {
                    final CommandSender console = Bukkit.getConsoleSender();
                    Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                        Bukkit.dispatchCommand(console, "delwarp " + name);
                        Schedulers.entity(plugin, viewer, () -> open(plugin, viewer));
                    });
                },
                () -> open(plugin, viewer)).open();
    }

    /** Closes the menu and tells the viewer how to create a warp (the name can't be prompted here). */
    private void promptCreate() {
        viewer.closeInventory();
        Msg.value(viewer, "To create a warp, run", "/setwarp <name>");
    }

    /** Builds the icon for warp {@code name}, its lore describing {@code location} and the actions. */
    private ItemStack icon(String name, Location location) {
        List<String> lore = List.of(
                Style.lore(describe(location)),
                Style.hint("Left-click to teleport"),
                Style.BAD + SmallCaps.of("Shift/right-click to delete"));
        return new ItemBuilder(AdminWarps.icon(plugin, name))
                .name(Style.button(Style.INFO, name))
                .lore(lore)
                .clean()
                .build();
    }

    /** The bottom-row "create warp at my location" button. */
    private static ItemStack createButton() {
        return new ItemBuilder(Material.EMERALD)
                .name(Style.button(Style.DEPOSIT, "Create warp at my location"))
                .lore(Style.hint("Run /setwarp <name>"))
                .clean()
                .build();
    }

    /** @return a human-readable {@code "world (x, y, z)"} description, or a fallback if unresolved. */
    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Location unavailable";
        }
        return location.getWorld().getName() + " (" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /** @return a row count (2-6): one navigation row plus enough content rows for {@code warpCount}. */
    private static int rows(int warpCount) {
        int contentRows = Math.max(1, (int) Math.ceil(warpCount / 9.0));
        return Math.min(6, Math.max(2, contentRows + 1));
    }
}
