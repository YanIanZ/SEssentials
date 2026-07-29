package dev.iyanz.sessentials.module.warp;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.gui.ItemBuilder;
import dev.iyanz.sessentials.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Chest GUI listing every warp: one ender-pearl icon per warp, its lore showing the
 * world and coordinates, clicking teleports the viewer there and closes the menu.
 *
 * <p>Reuses {@link Warps} for persistence, exactly like the {@code /warp} and
 * {@code /warps} text commands. Not paginated: only the first
 * {@value #MAX_ROWS} &times; {@value #COLUMNS} warps are shown.</p>
 *
 * <p>Opened by {@code /warps}; see {@link WarpModule}.</p>
 */
public final class WarpsMenu extends Menu {

    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;

    private final List<String> names;

    private WarpsMenu(SEssentialsPlugin plugin, Player viewer, List<String> names) {
        super(plugin, viewer, MM.deserialize(Style.title("Warps")), rows(names.size()));
        this.names = names;
    }

    /**
     * Builds and opens the warps menu for {@code viewer}. Sends an informational
     * message instead of an empty menu if no warps are set.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        List<String> names = new ArrayList<>(Warps.names(plugin));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        if (names.isEmpty()) {
            Msg.info(viewer, "There are no warps set.");
            return;
        }
        new WarpsMenu(plugin, viewer, names).open();
    }

    @Override
    protected void build() {
        int size = Math.min(names.size(), MAX_ROWS * COLUMNS);
        for (int slot = 0; slot < size; slot++) {
            String name = names.get(slot);
            Location location = Warps.location(plugin, name);
            set(slot, icon(name, location), event -> teleport(name, location));
        }
    }

    /** Builds the ender-pearl icon for warp {@code name}, its lore describing {@code location}. */
    private ItemStack icon(String name, Location location) {
        List<String> lore = new ArrayList<>();
        lore.add(Style.lore(describe(location)));
        lore.add(Style.hint("Click to teleport"));
        return new ItemBuilder(Material.ENDER_PEARL)
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

    /** @return a human-readable {@code "world (x, y, z)"} description, or a fallback if unresolved. */
    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Location unavailable";
        }
        return location.getWorld().getName() + " (" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /** @return a row count (1-{@value #MAX_ROWS}) sized to fit {@code warpCount} icons. */
    private static int rows(int warpCount) {
        return Math.max(1, Math.min(MAX_ROWS, (warpCount + COLUMNS - 1) / COLUMNS));
    }
}
