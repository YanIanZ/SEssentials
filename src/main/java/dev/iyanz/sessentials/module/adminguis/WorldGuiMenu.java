package dev.iyanz.sessentials.module.adminguis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Clean-room, CMI-style <em>world teleport</em> GUI, built on SELIB's {@link PagedMenu}. Lists
 * every loaded world ({@link Bukkit#getWorlds()}) as a paginated grid of environment-themed
 * icons: overworld → {@link Material#GRASS_BLOCK}, nether → {@link Material#NETHERRACK},
 * end → {@link Material#END_STONE} (any other/custom environment falls back to grass). Each
 * icon's lore shows the world's environment and its current player count.
 *
 * <p><strong>Reuse-only front-end.</strong> A left-click teleports the viewer to the world by
 * dispatching the existing, already-audited {@code /worldtp <world>} command via
 * {@code viewer.performCommand(...)}, then closes the menu. The world's time and weather are
 * never touched. A bottom-row close button dismisses the menu.</p>
 *
 * <p>Folia-safe: the menu is opened and its clicks routed on the viewer's region thread, and
 * {@code performCommand} runs on that same thread — exactly as if the viewer had typed the
 * command.</p>
 *
 * <p>Opened by {@code /worldgui}; see {@link AdminGuisModule}.</p>
 */
public final class WorldGuiMenu extends PagedMenu {

    /** Bottom-row slot for the close button (between the page indicator and the next button). */
    private static final int CLOSE_OFFSET = 6;

    private WorldGuiMenu(SEssentialsPlugin plugin, Player viewer, int rows) {
        super(plugin, viewer, MM.deserialize(Style.title("Worlds")), rows);
    }

    /**
     * Builds and opens the world GUI for {@code viewer}.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        new WorldGuiMenu(plugin, viewer, rows(Bukkit.getWorlds().size())).open();
    }

    @Override
    protected void build() {
        // PagedMenu lays out the content grid and the bottom navigation row (prev / indicator /
        // next, padded with filler); add the close button into a free navigation-row gap.
        super.build();
        final int bottom = getInventory().getSize() - 9;
        set(bottom + CLOSE_OFFSET, Buttons.close(), event -> viewer.closeInventory());
    }

    @Override
    protected List<PageEntry> entries() {
        List<World> worlds = Bukkit.getWorlds();
        List<PageEntry> entries = new ArrayList<>(worlds.size());
        for (World world : worlds) {
            entries.add(new PageEntry(icon(world), event -> onClick(event, world.getName())));
        }
        return entries;
    }

    /** Routes a world click: a left/normal click teleports; other clicks are ignored. */
    private void onClick(InventoryClickEvent event, String worldName) {
        if (!event.isLeftClick()) {
            return;
        }
        viewer.closeInventory();
        // Reuse the audited /worldtp command; never touch world time/weather here.
        viewer.performCommand("worldtp " + worldName);
    }

    /** Builds the environment-themed icon for {@code world}, lore = environment + player count. */
    private ItemStack icon(World world) {
        int players = world.getPlayers().size();
        return new ItemBuilder(iconMaterial(world.getEnvironment()))
                .name(Style.button(Style.INFO, world.getName()))
                .lore(
                        Style.labelled("Environment:", environmentLabel(world.getEnvironment())),
                        Style.labelled("Players:", Integer.toString(players)),
                        Style.hint("Left-click to teleport"))
                .clean()
                .build();
    }

    /** @return the block material representing {@code environment} (grass block as the fallback). */
    private static Material iconMaterial(World.Environment environment) {
        return switch (environment) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    /** @return a human-readable label for {@code environment} (e.g. {@code "The End"}). */
    private static String environmentLabel(World.Environment environment) {
        return switch (environment) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "The End";
            default -> titleCase(environment.name());
        };
    }

    /** @return {@code raw} with underscores turned to spaces and each word title-cased. */
    private static String titleCase(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(raw.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /** @return a row count (2-6): one navigation row plus enough content rows for {@code worldCount}. */
    private static int rows(int worldCount) {
        int contentRows = Math.max(1, (int) Math.ceil(worldCount / 9.0));
        return Math.min(6, Math.max(2, contentRows + 1));
    }
}
