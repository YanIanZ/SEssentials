package dev.iyanz.sessentials.module.menus;

import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB single-row chest GUI for switching the viewer's own game mode. Each of the four
 * vanilla modes (survival, creative, adventure, spectator) is a clickable icon; clicking one
 * sets the viewer's game mode on their region thread, closes the menu and confirms in chat.
 *
 * <p>Only the modes the viewer is allowed to use are shown. Visibility reuses the existing
 * game-mode permission convention: {@code sessentials.gamemode} (grants every mode) or the
 * per-mode shortcut permissions {@code sessentials.gms} / {@code .gmc} / {@code .gma} /
 * {@code .gmsp}. A viewer who holds none of those granular permissions but could open the menu
 * (permission {@code sessentials.gmmenu}) is shown all four modes, so the menu is never empty.</p>
 *
 * <p>Opened by {@code /gmmenu}; see {@link MenusModule}. Folia-safe via the {@link Menu} base:
 * the inventory opens on the viewer's region thread and clicks are cancelled and dispatched
 * there, and the game-mode mutation is additionally routed through {@link Schedulers#entity}.</p>
 */
public final class GameModeMenu extends Menu {

    /** Modes in display order, each mapped to its icon slot, material and shortcut permission. */
    private enum Mode {
        SURVIVAL(GameMode.SURVIVAL, 1, Material.DIAMOND_SWORD, "sessentials.gms"),
        CREATIVE(GameMode.CREATIVE, 3, Material.GRASS_BLOCK, "sessentials.gmc"),
        ADVENTURE(GameMode.ADVENTURE, 5, Material.FILLED_MAP, "sessentials.gma"),
        SPECTATOR(GameMode.SPECTATOR, 7, Material.ENDER_EYE, "sessentials.gmsp");

        private final GameMode gameMode;
        private final int slot;
        private final Material icon;
        private final String shortcutPermission;

        Mode(GameMode gameMode, int slot, Material icon, String shortcutPermission) {
            this.gameMode = gameMode;
            this.slot = slot;
            this.icon = icon;
            this.shortcutPermission = shortcutPermission;
        }

        private String display() {
            String raw = gameMode.name().toLowerCase(Locale.ROOT);
            return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        }
    }

    private GameModeMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Game Mode")), 1);
    }

    /**
     * Builds and opens the game-mode menu for {@code viewer}.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, Player viewer) {
        new GameModeMenu(plugin, viewer).open();
    }

    @Override
    protected void build() {
        clear();
        final boolean granular = holdsGranularPermission();
        for (Mode mode : Mode.values()) {
            if (mayUse(mode, granular)) {
                final boolean active = viewer.getGameMode() == mode.gameMode;
                set(mode.slot, icon(mode, active), event -> apply(mode));
            }
        }
        fill(Buttons.filler());
    }

    /** Builds the icon for {@code mode}, glowing when it is the viewer's current mode. */
    private ItemStack icon(Mode mode, boolean active) {
        ItemBuilder builder = new ItemBuilder(mode.icon)
                .name(Style.button(Style.INFO, mode.display()))
                .lore(active ? Style.lore("Current mode") : Style.hint("Click to switch"))
                .clean();
        if (active) {
            builder.glow();
        }
        return builder.build();
    }

    /** Sets the viewer's game mode on their region thread, then closes and confirms. */
    private void apply(Mode mode) {
        final String display = mode.display();
        Schedulers.entity(plugin, viewer, () -> {
            viewer.setGameMode(mode.gameMode);
            viewer.closeInventory();
            Msg.ok(viewer, "Game mode set to " + display.toLowerCase(Locale.ROOT) + ".");
        });
    }

    /**
     * @param mode     the mode whose visibility is being decided
     * @param granular whether the viewer holds any granular game-mode permission
     * @return {@code true} if {@code mode} should be shown to the viewer
     */
    private boolean mayUse(Mode mode, boolean granular) {
        if (viewer.hasPermission("sessentials.gamemode") || viewer.hasPermission(mode.shortcutPermission)) {
            return true;
        }
        // Fallback: a viewer with only sessentials.gmmenu (no granular perms) may use every mode.
        return !granular;
    }

    /** @return {@code true} if the viewer holds {@code sessentials.gamemode} or any per-mode shortcut. */
    private boolean holdsGranularPermission() {
        if (viewer.hasPermission("sessentials.gamemode")) {
            return true;
        }
        for (Mode mode : Mode.values()) {
            if (viewer.hasPermission(mode.shortcutPermission)) {
                return true;
            }
        }
        return false;
    }
}
