package dev.iyanz.sessentials.module.hub;

import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB chest GUI that acts as the central hub for every other SEssentials sub-GUI (clean-room,
 * own layout). It shows a grid of themed icons; clicking one opens that feature's existing menu by
 * dispatching the viewer's own command with {@link Player#performCommand(String)} — the hub itself
 * implements no feature logic.
 *
 * <p>Each entry is shown only when {@link Entry#isVisibleTo(Player)} holds: the target command must
 * be registered on the server (checked against the live command map) and, when the command is
 * permission-gated, the viewer must hold that permission. Visible icons are laid out centred inside
 * the chest so any subset renders tidily.</p>
 *
 * <p>Folia-safe: the base {@link Menu} opens on the viewer's region thread and routes clicks there;
 * {@link #launch(String)} runs on that same thread, so the reused sub-GUI opens from the correct
 * region.</p>
 */
public final class HubMenu extends Menu {

    private static final int ROWS = 6;
    /** Number of usable inner rows (the outer ring is a border). */
    private static final int INNER_ROWS = 4;
    /** Number of usable inner columns. */
    private static final int INNER_COLS = 7;
    /** Bottom-centre close button. */
    private static final int SLOT_CLOSE = 49;

    /**
     * The hub's link targets, in display order. Only entries whose command exists <em>and</em>
     * whose permission the viewer holds are rendered. Every command here opens a GUI with no
     * required arguments, so a single click reliably launches it. Commands that need an argument
     * (e.g. {@code /manage <player>}, {@code /trade <player>}) are intentionally omitted — the
     * player-facing {@code /players} list already fronts {@code /manage}.
     */
    private static final List<Entry> ENTRIES = List.of(
            new Entry("tpmenu", "sessentials.tpmenu", Material.ENDER_PEARL, Style.INFO,
                    "Teleport", "Open the teleport menu"),
            new Entry("homes", "sessentials.homes", Material.RED_BED, Style.DEPOSIT,
                    "Homes", "Manage your homes"),
            new Entry("warps", "sessentials.warps", Material.LODESTONE, Style.INFO,
                    "Warps", "Browse server warps"),
            new Entry("kits", null, Material.CHEST, Style.DEPOSIT,
                    "Kits", "Claim available kits"),
            new Entry("backpack", "sessentials.backpack", Material.SHULKER_BOX, Style.HISTORY,
                    "Backpack", "Open your backpack"),
            new Entry("enchantgui", "sessentials.enchantgui", Material.ENCHANTING_TABLE, Style.UPGRADE,
                    "Enchanting", "Enchant your held item"),
            new Entry("players", "sessentials.playerlist", Material.PLAYER_HEAD, Style.BRAND,
                    "Players", "View online players"),
            new Entry("settings", "sessentials.settings", Material.COMPARATOR, Style.INFO,
                    "Settings", "Your personal toggles"),
            new Entry("warpadmin", "sessentials.warpadmin", Material.COMMAND_BLOCK, Style.UPGRADE,
                    "Warp Admin", "Manage server warps"),
            new Entry("itemsgui", "sessentials.itembrowser", Material.ITEM_FRAME, Style.HINT,
                    "Items", "Browse & grab items"),
            new Entry("worldgui", "sessentials.worldgui", Material.GRASS_BLOCK, Style.DEPOSIT,
                    "Worlds", "Teleport between worlds"),
            new Entry("gmmenu", "sessentials.gmmenu", Material.GRASS_BLOCK, Style.INFO,
                    "Game Mode", "Change your game mode"));

    /**
     * @param plugin the owning plugin
     * @param viewer the player opening the hub
     */
    public HubMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, title(), ROWS);
    }

    private static Component title() {
        return MM.deserialize(Style.title("Essentials Menu"));
    }

    @Override
    protected void build() {
        border(Buttons.filler());

        final List<Entry> visible = new ArrayList<>();
        for (final Entry entry : ENTRIES) {
            if (entry.isVisibleTo(viewer)) {
                visible.add(entry);
            }
        }
        placeCentered(visible);

        set(SLOT_CLOSE, Buttons.close(), e -> viewer.closeInventory());
        fillEmpty(Buttons.filler());
    }

    /**
     * Lays the visible icons out in centred rows within the inner {@link #INNER_ROWS} x
     * {@link #INNER_COLS} area, so any count renders as a balanced block.
     */
    private void placeCentered(List<Entry> visible) {
        final int total = visible.size();
        if (total == 0) {
            return;
        }
        final int rows = Math.min(INNER_ROWS, (int) Math.ceil(total / (double) INNER_COLS));
        final int startRow = 1 + Math.max(0, (INNER_ROWS - rows)) / 2;
        int idx = 0;
        for (int r = 0; r < rows && idx < total; r++) {
            final int inThisRow = Math.min(INNER_COLS, total - r * INNER_COLS);
            final int startCol = 1 + (INNER_COLS - inThisRow) / 2;
            final int invRow = startRow + r;
            for (int c = 0; c < inThisRow; c++) {
                final Entry entry = visible.get(idx++);
                set(invRow * 9 + startCol + c, entry.icon(), e -> launch(entry.command()));
            }
        }
    }

    /**
     * Closes the hub and dispatches the viewer's own command to open the chosen sub-GUI. Runs on
     * the viewer's region thread (the click handler already does), exactly as if the viewer had
     * typed the command.
     */
    private void launch(String command) {
        viewer.closeInventory();
        viewer.performCommand(command);
    }

    /**
     * One hub link: the command it opens, an optional permission gate, and its themed icon.
     *
     * @param command     the command to run (no leading slash, no arguments)
     * @param permission  the permission to gate on, or {@code null} if the command is open to all
     * @param material    the icon material
     * @param color       a {@link Style} gradient fragment for the label
     * @param label       the plain button label (rendered in small caps)
     * @param description the plain lore description (rendered in small caps)
     */
    private record Entry(String command, String permission, Material material, String color,
                         String label, String description) {

        /** @return {@code true} if this command is registered and the viewer may run it. */
        boolean isVisibleTo(Player viewer) {
            if (Bukkit.getCommandMap().getCommand(command) == null) {
                return false;
            }
            return permission == null || viewer.hasPermission(permission);
        }

        /** @return a fresh themed icon for this entry. */
        ItemStack icon() {
            return new ItemBuilder(material)
                    .name(Style.button(color, label))
                    .lore(Style.lore(description), Style.hint("Click to open"))
                    .clean()
                    .build();
        }
    }
}
