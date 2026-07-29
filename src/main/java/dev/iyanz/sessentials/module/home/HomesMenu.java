package dev.iyanz.sessentials.module.home;

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
 * Chest GUI listing {@code viewer}'s homes: one bed icon per home, its lore showing
 * the world and coordinates, clicking teleports the viewer there and closes the menu.
 *
 * <p>Reuses {@link Homes} for persistence, exactly like the {@code /home} and
 * {@code /homes} text commands. Not paginated: only the first {@value #MAX_ROWS}
 * &times; {@value #COLUMNS} homes are shown, which comfortably covers any realistic
 * per-player home limit.</p>
 *
 * <p>Opened by {@code /homes}; see {@link HomeModule}.</p>
 */
public final class HomesMenu extends Menu {

    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;

    private final List<String> names;

    private HomesMenu(SEssentialsPlugin plugin, Player viewer, List<String> names) {
        super(plugin, viewer, MM.deserialize(Style.title("Your Homes")), rows(names.size()));
        this.names = names;
    }

    /**
     * Builds and opens the homes menu for {@code viewer}. Sends an informational
     * message instead of an empty menu if they have no homes set.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        List<String> names = new ArrayList<>(Homes.names(plugin, viewer.getUniqueId()));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        if (names.isEmpty()) {
            Msg.info(viewer, "You have no homes set.");
            return;
        }
        new HomesMenu(plugin, viewer, names).open();
    }

    @Override
    protected void build() {
        int size = Math.min(names.size(), MAX_ROWS * COLUMNS);
        for (int slot = 0; slot < size; slot++) {
            String name = names.get(slot);
            Location location = Homes.location(plugin, viewer.getUniqueId(), name);
            set(slot, icon(name, location), event -> teleport(name, location));
        }
    }

    /** Builds the bed icon for home {@code name}, its lore describing {@code location}. */
    private ItemStack icon(String name, Location location) {
        List<String> lore = new ArrayList<>();
        lore.add(Style.lore(describe(location)));
        lore.add(Style.hint("Click to teleport"));
        return new ItemBuilder(Material.RED_BED)
                .name(Style.button(Style.INFO, name))
                .lore(lore)
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

    /** @return a human-readable {@code "world (x, y, z)"} description, or a fallback if unresolved. */
    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Location unavailable";
        }
        return location.getWorld().getName() + " (" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /** @return a row count (1-{@value #MAX_ROWS}) sized to fit {@code homeCount} icons. */
    private static int rows(int homeCount) {
        return Math.max(1, Math.min(MAX_ROWS, (homeCount + COLUMNS - 1) / COLUMNS));
    }
}
