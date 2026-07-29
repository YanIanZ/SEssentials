package dev.iyanz.sessentials.module.warp;

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
 * Paginated chest GUI listing every warp, built on SELIB's {@link PagedMenu}. Each warp is
 * one {@link PageEntry}: a themed icon (stored material, else {@link Material#ENDER_PEARL})
 * whose lore shows the world, rounded coordinates and a click hint. A left/normal click
 * teleports the viewer there and closes the menu. Senders holding the warp-admin permission
 * ({@code sessentials.delwarp}) may shift-click to open a {@link ConfirmMenu} that deletes
 * the warp and reopens this list. Pagination is automatic once the warps overflow the grid.
 *
 * <p>Reuses {@link Warps} for all persistence and the {@link Player#teleportAsync teleportAsync}
 * path for Folia-safe, region-hopping teleports — only the presentation changed.</p>
 *
 * <p>Opened by {@code /warps}; see {@link WarpModule}.</p>
 */
public final class WarpsMenu extends PagedMenu {

    /** Permission gating shift-click warp deletion (the same node {@code /delwarp} requires). */
    private static final String ADMIN_PERMISSION = "sessentials.delwarp";

    private WarpsMenu(SEssentialsPlugin plugin, Player viewer, int rows) {
        super(plugin, viewer, MM.deserialize(Style.title("Warps")), rows);
    }

    /**
     * Builds and opens the warps menu for {@code viewer}. Sends an informational message
     * instead of an empty menu if no warps are set.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        List<String> names = new ArrayList<>(Warps.names(plugin));
        if (names.isEmpty()) {
            Msg.info(viewer, "There are no warps set.");
            return;
        }
        new WarpsMenu(plugin, viewer, rows(names.size())).open();
    }

    @Override
    protected List<PageEntry> entries() {
        List<String> names = new ArrayList<>(Warps.names(plugin));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        boolean admin = viewer.hasPermission(ADMIN_PERMISSION);

        List<PageEntry> entries = new ArrayList<>(names.size());
        for (String name : names) {
            Location location = Warps.location(plugin, name);
            entries.add(new PageEntry(icon(name, location, admin), event -> onClick(event, name, location, admin)));
        }
        return entries;
    }

    /** Routes a warp click: admins shift-click to delete (with confirmation), any other click warps. */
    private void onClick(InventoryClickEvent event, String name, Location location, boolean admin) {
        if (admin && event.isShiftClick()) {
            confirmDelete(name);
        } else {
            teleport(name, location);
        }
    }

    /** Builds the ender-pearl icon for warp {@code name}, its lore describing {@code location}. */
    private ItemStack icon(String name, Location location, boolean admin) {
        List<String> lore = new ArrayList<>(3);
        lore.add(Style.lore(describe(location)));
        lore.add(Style.hint("Click to warp"));
        if (admin) {
            lore.add(Style.BAD + SmallCaps.of("Shift-click to delete"));
        }
        return new ItemBuilder(Warps.icon(plugin, name))
                .name(Style.button(Style.INFO, name))
                .lore(lore)
                .clean()
                .build();
    }

    /** Teleports the viewer to {@code location} (re-fetched at build time) and closes the menu. */
    private void teleport(String name, Location location) {
        if (location == null) {
            Msg.err(viewer, "No warp named \"" + name + "\".");
            return;
        }
        viewer.closeInventory();
        viewer.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(viewer, "Warped to \"" + name + "\".");
            } else {
                Msg.err(viewer, "Teleport to warp \"" + name + "\" failed.");
            }
        });
    }

    /** Opens a confirmation dialog that deletes warp {@code name} and reopens this menu. */
    private void confirmDelete(String name) {
        ItemStack prompt = new ItemBuilder(Warps.icon(plugin, name))
                .name(Style.button(Style.DANGER, "Delete " + name))
                .lore(Style.hint("This cannot be undone"))
                .clean()
                .build();
        new ConfirmMenu(plugin, viewer,
                MM.deserialize(Style.title("Delete Warp?")),
                prompt,
                () -> {
                    Warps.delete(plugin, name);
                    Msg.ok(viewer, "Warp \"" + name + "\" deleted.");
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

    /** @return a row count (2-6) sized to fit {@code warpCount} warps. */
    private static int rows(int warpCount) {
        int contentRows = Math.max(1, (int) Math.ceil(warpCount / 9.0));
        return Math.min(6, contentRows + 1);
    }
}
