package dev.iyanz.sessentials.module.tpmenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Paginated sub-page of the {@link TpMenu teleport hub}: every online player (except the viewer)
 * rendered as a textured head. Clicking a head closes the menu and dispatches {@code /tpa <name>}
 * as the viewer, reusing that command's audited, Folia-safe teleport-request logic and permission
 * checks rather than re-implementing anything. Built on SELIB's {@link PagedMenu}, so the list
 * paginates automatically once it overflows the grid; a back arrow returns to the hub and a close
 * button dismisses the menu.
 *
 * <p>Folia-safe: the base menu opens on the viewer's region thread and routes clicks there. Only
 * thread-safe player data is read while building icons — names for display and each player's profile
 * for the head skin — never another player's live world/entity state off its region thread.</p>
 */
public final class TpPlayersMenu extends PagedMenu {

    /** Nav-row filler slot (left of the page indicator) used for the back arrow. */
    private static final int NAV_BACK_OFFSET = 2;
    /** Nav-row filler slot (right of the page indicator) used for the close button. */
    private static final int NAV_CLOSE_OFFSET = 6;

    private TpPlayersMenu(SEssentialsPlugin plugin, Player viewer, int rows) {
        super(plugin, viewer, MM.deserialize(Style.title("Teleport to Player")), rows);
    }

    /**
     * Opens the online-players sub-page for {@code viewer}. Sends an informational message instead of
     * an empty menu when no other players are online.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    static void open(SEssentialsPlugin plugin, Player viewer) {
        final List<Player> others = onlineOthers(viewer);
        if (others.isEmpty()) {
            Msg.info(viewer, "No other players are online to teleport to.");
            return;
        }
        new TpPlayersMenu(plugin, viewer, rows(others.size())).open();
    }

    @Override
    protected List<PageEntry> entries() {
        final List<Player> others = onlineOthers(viewer);
        others.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        final List<PageEntry> entries = new ArrayList<>(others.size());
        for (final Player target : others) {
            final String name = target.getName();
            entries.add(new PageEntry(head(target), event -> request(name)));
        }
        return entries;
    }

    @Override
    protected void build() {
        super.build();
        // Overlay a back arrow and a close button onto the navigation row's filler slots. These
        // offsets never collide with the prev/indicator/next slots the base fills.
        final int navRowStart = getInventory().getSize() - 9;
        set(navRowStart + NAV_BACK_OFFSET, Buttons.back(), event -> new TpMenu(plugin, viewer).open());
        set(navRowStart + NAV_CLOSE_OFFSET, Buttons.close(), event -> viewer.closeInventory());
    }

    /**
     * Closes the menu, then dispatches {@code /tpa <name>} as the viewer on their own region thread
     * (this click handler already runs there), reusing the audited teleport-request command.
     */
    private void request(String name) {
        viewer.closeInventory();
        viewer.performCommand(TpMenu.TPA_COMMAND + " " + name);
    }

    /** Builds the textured head icon for {@code target}. */
    private ItemStack head(Player target) {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(Style.button(Style.BRAND, target.getName()))
                .lore(Style.lore("Send them a teleport request"), Style.hint("Click to send a request"))
                .head(target)
                .build();
    }

    /** @return every online player except {@code viewer} (name and profile reads are thread-safe). */
    private static List<Player> onlineOthers(Player viewer) {
        final List<Player> others = new ArrayList<>();
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(viewer.getUniqueId())) {
                others.add(online);
            }
        }
        return others;
    }

    /** @return a row count (2-6) sized to fit {@code count} player heads plus the navigation row. */
    private static int rows(int count) {
        final int contentRows = Math.max(1, (int) Math.ceil(count / 9.0));
        return Math.min(6, contentRows + 1);
    }
}
