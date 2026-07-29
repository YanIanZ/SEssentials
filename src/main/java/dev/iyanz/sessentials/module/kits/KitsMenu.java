package dev.iyanz.sessentials.module.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.gui.ItemBuilder;
import dev.iyanz.sessentials.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Chest GUI listing the kits {@code viewer} has permission to use: one chest icon
 * per kit, its lore showing whether it's ready or the remaining cooldown, clicking
 * claims it.
 *
 * <p>All cooldown bookkeeping and item-granting is delegated to {@link KitService}
 * (the same methods {@link KitCommands} calls for {@code /kit}), so this menu never
 * re-implements that logic — it only checks the current status and reacts.</p>
 *
 * <p>Opened by {@code /kits} (alias {@code /kitlist}); see {@link KitsModule} /
 * {@link KitCommands}.</p>
 */
public final class KitsMenu extends Menu {

    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;

    private final KitService service;
    private final List<String> names;

    private KitsMenu(SEssentialsPlugin plugin, Player viewer, KitService service, List<String> names) {
        super(plugin, viewer, MM.deserialize(Style.title("Kits")), rows(names.size()));
        this.service = service;
        this.names = names;
    }

    /**
     * Builds and opens the kits menu for {@code viewer}, showing only the kits they
     * have permission to use ({@code sessentials.kit.<name>}). Sends an
     * informational message instead of an empty menu if none are available.
     *
     * @param plugin  the owning plugin
     * @param service the shared kit service (definitions + cooldowns)
     * @param viewer  the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, KitService service, Player viewer) {
        List<String> names = new ArrayList<>();
        for (String name : service.names()) {
            if (viewer.hasPermission(KitService.permission(name))) {
                names.add(name);
            }
        }
        if (names.isEmpty()) {
            Msg.info(viewer, "There are no kits available to you.");
            return;
        }
        new KitsMenu(plugin, viewer, service, names).open();
    }

    @Override
    protected void build() {
        int size = Math.min(names.size(), MAX_ROWS * COLUMNS);
        for (int slot = 0; slot < size; slot++) {
            String name = names.get(slot);
            set(slot, icon(name), event -> claim(name));
        }
    }

    /** Builds the chest icon for kit {@code name}, coloured and labelled by its live cooldown status. */
    private ItemStack icon(String name) {
        long remaining = service.remainingSeconds(viewer.getUniqueId(), name);
        boolean ready = remaining <= 0;
        String color = ready ? Style.DEPOSIT : Style.DANGER;
        String status = ready ? "Ready" : KitService.formatDuration(remaining);

        List<String> lore = new ArrayList<>();
        lore.add(Style.lore("Cooldown: " + status));
        if (ready) {
            lore.add(Style.hint("Click to claim"));
        }

        return new ItemBuilder(Material.CHEST)
                .name(Style.button(color, name))
                .lore(lore)
                .clean()
                .build();
    }

    /**
     * Re-checks the kit's cooldown at click time (the lore may be stale) and, if
     * ready, starts its cooldown and grants its items via {@link KitService};
     * otherwise errors via {@link Msg}, exactly like {@code /kit <name>}.
     */
    private void claim(String name) {
        long remaining = service.remainingSeconds(viewer.getUniqueId(), name);
        if (remaining > 0) {
            Msg.err(viewer, "Kit " + name + " is on cooldown for " + KitService.formatDuration(remaining) + ".");
            return;
        }
        Optional<Kit> kitOpt = service.get(name);
        if (kitOpt.isEmpty()) {
            Msg.err(viewer, "No such kit: " + name + ".");
            return;
        }
        Kit kit = kitOpt.get();
        service.startCooldown(viewer.getUniqueId(), name, kit.cooldownSeconds());
        service.grant(viewer, kit.items());
        Msg.ok(viewer, "You received kit " + name + ".");
        viewer.closeInventory();
    }

    /** @return a row count (1-{@value #MAX_ROWS}) sized to fit {@code kitCount} icons. */
    private static int rows(int kitCount) {
        return Math.max(1, Math.min(MAX_ROWS, (kitCount + COLUMNS - 1) / COLUMNS));
    }
}
