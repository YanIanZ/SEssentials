package dev.iyanz.sessentials.module.adminguis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ConfirmMenu;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Clean-room, CMI-style <em>home admin</em> GUI, built on SELIB's {@link PagedMenu}. Lists a
 * target player's homes — read straight from the shared {@code home} data store at
 * {@code "<uuid>.homes.<name>"} — as a paginated grid of {@link Material#RED_BED} icons whose
 * lore shows each home's world and rounded coordinates.
 *
 * <p>The target is addressed purely by UUID (resolved by the command from an
 * {@link org.bukkit.OfflinePlayer}), so this works whether or not the owner is online, and no
 * entity/online API is ever touched on the owner.</p>
 *
 * <ul>
 *   <li><b>Left-click</b> a home → the <em>viewer</em> is teleported to the stored location via
 *       {@link Player#teleportAsync teleportAsync}. The location is re-parsed from the store at
 *       click time (parse-safe: a {@code null} — unloaded world or malformed entry — is reported
 *       rather than acted on).</li>
 *   <li><b>Shift- or right-click</b> a home → a {@link ConfirmMenu}; confirming removes the
 *       {@code "<uuid>.homes.<name>"} entry from the store, saves, and reopens this list.</li>
 * </ul>
 *
 * <p>Folia-safe: the menu is opened and its clicks routed on the viewer's region thread, so both
 * the teleport and the store mutation already run on that thread; the store's own
 * {@link YamlStore#save()} offloads only the disk write. No {@code Bukkit.getScheduler()} is used.</p>
 *
 * <p>Opened by {@code /homeadmin <player>}; see {@link AdminGuisModule}.</p>
 */
public final class HomeAdminMenu extends PagedMenu {

    private final UUID targetUuid;
    private final String targetName;

    private HomeAdminMenu(SEssentialsPlugin plugin, Player viewer, UUID targetUuid, String targetName, int rows) {
        super(plugin, viewer, MM.deserialize(Style.title(targetName + " Homes")), rows);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    /**
     * Builds and opens the home-admin menu for {@code viewer}, showing {@code targetUuid}'s homes.
     * Sends an informational message instead of an empty menu if the target has no homes.
     *
     * @param plugin     the owning plugin
     * @param viewer     the staff member viewing (and teleporting on click)
     * @param targetUuid the owning player's UUID (the store key)
     * @param targetName the owning player's display name (for the title/messages)
     */
    public static void open(SEssentialsPlugin plugin, Player viewer, UUID targetUuid, String targetName) {
        List<String> names = homeStore(plugin).keys(targetUuid + ".homes");
        if (names.isEmpty()) {
            Msg.info(viewer, targetName + " has no homes.");
            return;
        }
        new HomeAdminMenu(plugin, viewer, targetUuid, targetName, rows(names.size())).open();
    }

    @Override
    protected List<PageEntry> entries() {
        YamlStore store = homeStore(plugin);
        List<String> names = new ArrayList<>(store.keys(targetUuid + ".homes"));
        names.sort(String.CASE_INSENSITIVE_ORDER);

        List<PageEntry> entries = new ArrayList<>(names.size());
        for (String name : names) {
            Location location = store.getLocation(homePath(name));
            entries.add(new PageEntry(icon(name, location), event -> onClick(event, name)));
        }
        return entries;
    }

    /** Routes a home click: shift- or right-click deletes (with confirmation), any other teleports. */
    private void onClick(InventoryClickEvent event, String name) {
        if (event.isShiftClick() || event.isRightClick()) {
            confirmDelete(name);
        } else {
            teleport(name);
        }
    }

    /**
     * Teleports the viewer to home {@code name}. The location is re-parsed from the store here so
     * the teleport is parse-safe (a {@code null} — unloaded world or malformed entry — is reported,
     * never acted on). Runs on the viewer's region thread (click handlers already do), and
     * {@link Player#teleportAsync teleportAsync} hops to the destination region as needed.
     */
    private void teleport(String name) {
        Location location = homeStore(plugin).getLocation(homePath(name));
        if (location == null) {
            Msg.err(viewer, "Home \"" + name + "\" has no reachable location.");
            return;
        }
        viewer.closeInventory();
        viewer.teleportAsync(location).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                Msg.ok(viewer, "Teleported to " + targetName + "'s home \"" + name + "\".");
            } else {
                Msg.err(viewer, "Teleport to home \"" + name + "\" failed.");
            }
        });
    }

    /**
     * Opens a confirmation dialog. Confirming removes {@code "<uuid>.homes.<name>"} from the store,
     * saves, and reopens this menu; cancelling simply reopens the list.
     */
    private void confirmDelete(String name) {
        ItemStack prompt = new ItemBuilder(Material.RED_BED)
                .name(Style.button(Style.DANGER, "Delete " + name))
                .lore(Style.hint("This cannot be undone"))
                .clean()
                .build();
        new ConfirmMenu(plugin, viewer,
                MM.deserialize(Style.title("Delete Home?")),
                prompt,
                () -> {
                    YamlStore store = homeStore(plugin);
                    store.remove(homePath(name));
                    store.save();
                    Msg.ok(viewer, "Deleted " + targetName + "'s home \"" + name + "\".");
                    open(plugin, viewer, targetUuid, targetName);
                },
                () -> open(plugin, viewer, targetUuid, targetName)).open();
    }

    /** Builds the bed icon for home {@code name}, its lore describing {@code location} and actions. */
    private ItemStack icon(String name, Location location) {
        return new ItemBuilder(Material.RED_BED)
                .name(Style.button(Style.INFO, name))
                .lore(
                        Style.lore(describe(location)),
                        Style.hint("Left-click to teleport"),
                        Style.BAD + SmallCaps.of("Shift/right-click to delete"))
                .clean()
                .build();
    }

    /** @return the store path for the target's home named {@code name}. */
    private String homePath(String name) {
        return targetUuid + ".homes." + name;
    }

    /** @return the shared {@code home} data store. */
    private static YamlStore homeStore(SEssentialsPlugin plugin) {
        return plugin.stores().get("home");
    }

    /** @return a human-readable {@code "world (x, y, z)"} description, or a fallback if unresolved. */
    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Location unavailable";
        }
        return location.getWorld().getName() + " (" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /** @return a row count (2-6): one navigation row plus enough content rows for {@code homeCount}. */
    private static int rows(int homeCount) {
        int contentRows = Math.max(1, (int) Math.ceil(homeCount / 9.0));
        return Math.min(6, Math.max(2, contentRows + 1));
    }
}
