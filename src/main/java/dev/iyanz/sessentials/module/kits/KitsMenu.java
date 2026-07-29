package dev.iyanz.sessentials.module.kits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Sounds;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB {@link PagedMenu} listing every defined kit: one entry per kit, its icon drawn from the
 * kit's first item (or a {@link Material#CHEST} fallback), its lore summarising the contents and
 * showing the live claim status — available, on cooldown, or locked by permission. Claimable kits
 * glow and can be claimed with a click.
 *
 * <p>All cooldown bookkeeping and item-granting is delegated to {@link KitService} (the same
 * methods {@link KitCommands} calls for {@code /kit}), so this menu never re-implements that
 * logic — it only reads the current status and reacts. Because {@link #entries()} is recomputed on
 * every (re)build, paging always reflects up-to-date permission and cooldown state.</p>
 *
 * <p>Opened by {@code /kits} (alias {@code /kitlist}); see {@link KitsModule} /
 * {@link KitCommands}.</p>
 */
public final class KitsMenu extends PagedMenu {

    private static final int ROWS = 6;

    private final KitService service;

    private KitsMenu(SEssentialsPlugin plugin, Player viewer, KitService service) {
        super(plugin, viewer, MM.deserialize(Style.title("Kits")), ROWS);
        this.service = service;
    }

    /**
     * Builds and opens the paginated kits browser for {@code viewer}, listing every defined kit
     * (each entry reflects whether the viewer may claim it). Sends an informational message
     * instead of an empty menu when no kits are defined at all.
     *
     * @param plugin  the owning plugin
     * @param service the shared kit service (definitions + cooldowns)
     * @param viewer  the player to show the menu to
     */
    public static void open(SEssentialsPlugin plugin, KitService service, Player viewer) {
        if (service.names().isEmpty()) {
            Msg.info(viewer, "There are no kits available.");
            return;
        }
        new KitsMenu(plugin, viewer, service).open();
    }

    @Override
    protected List<PageEntry> entries() {
        List<PageEntry> out = new ArrayList<>();
        for (String name : service.names()) {
            out.add(entryFor(name));
        }
        return out;
    }

    /** Builds the page entry for kit {@code name}, coloured and glowing by its live claim status. */
    private PageEntry entryFor(String name) {
        boolean permitted = viewer.hasPermission(KitService.permission(name));
        long remaining = service.remainingSeconds(viewer.getUniqueId(), name);
        boolean claimable = permitted && remaining <= 0;

        Optional<Kit> kitOpt = service.get(name);
        List<ItemStack> items = kitOpt.isPresent() ? kitOpt.get().items() : List.of();
        Material iconMaterial = !items.isEmpty() && items.get(0) != null
                ? items.get(0).getType()
                : Material.CHEST;
        String nameColor = claimable ? Style.DEPOSIT : (permitted ? Style.DANGER : Style.GRAY);

        List<String> lore = new ArrayList<>();
        lore.add(Style.lore(items.size() + " item(s)"));
        if (!permitted) {
            lore.add(Style.BAD + SmallCaps.of("No permission"));
        } else if (remaining > 0) {
            lore.add(Style.BAD + SmallCaps.of("On cooldown: ") + KitService.formatDuration(remaining));
        } else {
            lore.add(Style.OK + SmallCaps.of("Available — click to claim"));
        }

        ItemBuilder builder = new ItemBuilder(iconMaterial)
                .name(Style.button(nameColor, name))
                .lore(lore)
                .clean();
        if (claimable) {
            builder.glow();
        }
        return new PageEntry(builder.build(), event -> claim(name));
    }

    /**
     * Re-checks the kit's permission and cooldown at click time (the lore may be stale) and, if
     * claimable, starts its cooldown and grants its items via {@link KitService} — exactly like
     * {@code /kit <name>} — then closes with a success chime. Otherwise it only errors via
     * {@link Msg} and keeps the menu open.
     */
    private void claim(String name) {
        if (!viewer.hasPermission(KitService.permission(name))) {
            Msg.err(viewer, "You don't have access to kit " + name + ".");
            return;
        }
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
        Sounds.coins(viewer);
        viewer.closeInventory();
    }
}
