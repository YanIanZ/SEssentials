package dev.iyanz.sessentials.module.tpmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB chest hub of teleport shortcuts (clean-room layout). The middle row holds the available
 * destination buttons, centred; a close button sits at the bottom centre and a grey border pads the
 * rest.
 *
 * <p>Each button dispatches an existing SEssentials command as the viewer via
 * {@link Player#performCommand(String)} — reusing that command's audited, Folia-safe logic and its
 * own permission checks instead of re-implementing any teleport:</p>
 * <ul>
 *   <li>Spawn &rarr; {@code /spawn}, Back &rarr; {@code /back}, Random TP &rarr; {@code /rtp}.</li>
 *   <li>Warps &rarr; {@code /warps} and Homes &rarr; {@code /homes} (each opens its own list GUI).</li>
 *   <li>Teleport to Player &rarr; opens {@link TpPlayersMenu}, a paginated sub-page of online players
 *       whose heads dispatch {@code /tpa <name>}.</li>
 * </ul>
 *
 * <p>Only buttons whose underlying command is registered on the server are shown (checked via the
 * server command map), so the hub degrades gracefully when a feature module is absent.</p>
 *
 * <p>Folia-safe: the base {@link Menu} opens on the viewer's region thread and routes clicks there,
 * so every {@code performCommand} runs on the viewer's own region thread.</p>
 */
public final class TpMenu extends Menu {

    private static final int ROWS = 3;
    /** First inner slot of the middle row (slots 10..16 are the seven inner content slots). */
    private static final int ROW_START = 10;
    /** Number of inner slots on the middle row available for centred buttons. */
    private static final int ROW_WIDTH = 7;
    /** Bottom-centre slot for the close button. */
    private static final int CLOSE_SLOT = 22;

    /**
     * One destination shortcut: the command dispatched on click plus its icon presentation. The
     * command is also the existence key checked against the server command map.
     */
    private record Dest(String command, String label, String description, String hint,
                        Material material, String color) {
    }

    /**
     * The destination shortcuts, in display order. Each {@code command} is an existing SEssentials
     * command dispatched verbatim as the viewer; buttons whose command is not registered are skipped.
     */
    private static final List<Dest> DESTINATIONS = List.of(
            new Dest("spawn", "Spawn", "Teleport to the server spawn", "Click to teleport",
                    Material.RESPAWN_ANCHOR, Style.BRAND),
            new Dest("back", "Back", "Return to your previous location", "Click to teleport",
                    Material.CLOCK, Style.INFO),
            new Dest("rtp", "Random Teleport", "Teleport to a random location", "Click to teleport",
                    Material.ENDER_PEARL, Style.INFO),
            new Dest("warps", "Warps", "Browse and visit server warps", "Click to open",
                    Material.NETHER_STAR, Style.UPGRADE),
            new Dest("homes", "Homes", "Browse and visit your homes", "Click to open",
                    Material.RED_BED, Style.DEPOSIT));

    /** The command opened by the online-players sub-page (one {@code /tpa <name>} per head). */
    static final String TPA_COMMAND = "tpa";

    /**
     * @param plugin the owning plugin
     * @param viewer the player opening the hub
     */
    public TpMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Teleport Hub")), ROWS);
    }

    @Override
    protected void build() {
        clear();

        final List<HubButton> buttons = new ArrayList<>();
        for (final Dest dest : DESTINATIONS) {
            if (commandExists(dest.command())) {
                buttons.add(new HubButton(destIcon(dest), event -> dispatch(dest.command())));
            }
        }
        if (commandExists(TPA_COMMAND)) {
            buttons.add(new HubButton(playersIcon(), event -> TpPlayersMenu.open(plugin, viewer)));
        }

        // Centre the available buttons within the seven inner slots of the middle row.
        final int count = Math.min(buttons.size(), ROW_WIDTH);
        final int offset = (ROW_WIDTH - count) / 2;
        for (int i = 0; i < count; i++) {
            final HubButton button = buttons.get(i);
            set(ROW_START + offset + i, button.icon(), button.onClick());
        }

        set(CLOSE_SLOT, Buttons.close(), event -> viewer.closeInventory());
        border(Buttons.filler());
    }

    /** A single hub icon and its click handler. */
    private record HubButton(ItemStack icon, Consumer<InventoryClickEvent> onClick) {
    }

    /**
     * Closes the hub, then dispatches {@code command} as the viewer on their own region thread
     * (this click handler already runs there), reusing that command's audited logic and permissions.
     */
    private void dispatch(String command) {
        viewer.closeInventory();
        viewer.performCommand(command);
    }

    private ItemStack destIcon(Dest dest) {
        return new ItemBuilder(dest.material())
                .name(Style.button(dest.color(), dest.label()))
                .lore(Style.lore(dest.description()), Style.hint(dest.hint()))
                .clean()
                .build();
    }

    private ItemStack playersIcon() {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(Style.button(Style.HISTORY, "Teleport to Player"))
                .lore(Style.lore("Send a teleport request to a player"), Style.hint("Click to choose a player"))
                .clean()
                .build();
    }

    /** @return {@code true} if a command with {@code name} is registered on the server. */
    private static boolean commandExists(String name) {
        return Bukkit.getCommandMap().getCommand(name) != null;
    }
}
