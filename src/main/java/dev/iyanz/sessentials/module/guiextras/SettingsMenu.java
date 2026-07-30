package dev.iyanz.sessentials.module.guiextras;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.SmallCaps;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * SELIB {@link Menu} opened by {@code /settings}: a personal control panel of the viewer's own
 * toggle commands. Each icon, when clicked, <em>dispatches the viewer's existing toggle command</em>
 * via {@link Player#performCommand(String)} — reusing that command's already-audited logic and
 * permission checks — then re-renders in place so the icon reflects the new state.
 *
 * <p>Only toggles the viewer has permission to run are shown. Where a toggle's on/off state can be
 * cheaply and reliably read from the public Bukkit API (flight, infinite night vision) the icon
 * glows and is labelled Enabled/Disabled; the remaining toggles keep their state in other modules'
 * private registries, so they render as neutral "click to toggle" icons rather than coupling this
 * menu to that internal state.</p>
 *
 * <p>Folia-safe: the menu opens on the viewer's region thread and every click callback already runs
 * there, so {@code performCommand} executes the toggle on the viewer's own region thread.</p>
 */
final class SettingsMenu extends Menu {

    private static final int ROWS = 5;
    /** A centred 3x3 block of slots (rows 2-4, columns 4-6) for up to nine toggle icons. */
    private static final int[] SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    /** Bottom-centre slot for the close button. */
    private static final int CLOSE_SLOT = 40;

    /**
     * One personal toggle: the command dispatched on click, the permission that gates it, its
     * display label/description, its icon material and an optional state probe used only to glow
     * and label the icon (never to change behaviour). A {@code null} probe means "state unknown".
     */
    private record Toggle(String command, String perm, String label, String description,
                          Material icon, Predicate<Player> state) {
    }

    /**
     * The toggles offered, in display order. Each {@code command} is an existing SEssentials
     * command with a no-argument self-toggle executor; each {@code perm} is that command's own
     * permission node. State probes are supplied only where the public API exposes the state.
     */
    private static final List<Toggle> TOGGLES = List.of(
            new Toggle("fly", "sessentials.fly", "Fly", "Toggle flight",
                    Material.FEATHER, Player::getAllowFlight),
            new Toggle("msgtoggle", "sessentials.msgtoggle", "Private Messages",
                    "Toggle receiving private messages", Material.WRITABLE_BOOK, null),
            new Toggle("tptoggle", "sessentials.tptoggle", "Teleport Requests",
                    "Toggle accepting teleport requests", Material.ENDER_PEARL, null),
            new Toggle("socialspy", "sessentials.socialspy", "Social Spy",
                    "Toggle watching others' private messages", Material.SPYGLASS, null),
            new Toggle("vanish", "sessentials.vanish", "Vanish",
                    "Toggle being hidden from other players", Material.GLASS, null),
            new Toggle("god", "sessentials.god", "God Mode",
                    "Toggle damage invulnerability", Material.TOTEM_OF_UNDYING, null),
            new Toggle("nightvision", "sessentials.nightvision", "Night Vision",
                    "Toggle permanent night vision", Material.GOLDEN_CARROT,
                    SettingsMenu::hasInfiniteNightVision),
            new Toggle("damageindicator", "sessentials.damageindicator", "Damage Indicators",
                    "Toggle floating damage numbers", Material.NAME_TAG, null),
            new Toggle("afk", "sessentials.afk", "AFK", "Toggle your AFK status",
                    Material.CLOCK, null));

    private SettingsMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Settings")), ROWS);
    }

    /**
     * Opens the personal settings panel for {@code viewer}.
     *
     * @param plugin the owning plugin
     * @param viewer the player to show the menu to
     */
    static void open(SEssentialsPlugin plugin, Player viewer) {
        new SettingsMenu(plugin, viewer).open();
    }

    @Override
    protected void build() {
        clear();

        final List<Toggle> visible = new ArrayList<>();
        for (final Toggle toggle : TOGGLES) {
            if (viewer.hasPermission(toggle.perm())) {
                visible.add(toggle);
            }
        }

        for (int i = 0; i < visible.size() && i < SLOTS.length; i++) {
            final Toggle toggle = visible.get(i);
            set(SLOTS[i], icon(toggle), event -> {
                // Reuse the existing command's audited logic; it runs on the viewer's own
                // region thread (this handler already does) and enforces its own permission.
                viewer.performCommand(toggle.command());
                refresh();
            });
        }

        set(CLOSE_SLOT, Buttons.close(), event -> viewer.closeInventory());
        fill(Buttons.filler());
    }

    /** Builds the icon for one toggle, glowing and labelled from its state probe when available. */
    private ItemStack icon(Toggle toggle) {
        final Boolean on = toggle.state() == null ? null : toggle.state().test(viewer);

        final ItemBuilder builder = new ItemBuilder(toggle.icon())
                .name(Style.button(on != null && on ? Style.OK : Style.INFO, toggle.label()));

        final List<String> lore = new ArrayList<>();
        lore.add(Style.lore(toggle.description()));
        if (on != null) {
            lore.add(on ? Style.OK + SmallCaps.of("Enabled") : Style.GRAY + SmallCaps.of("Disabled"));
        }
        lore.add(Style.hint("Click to toggle"));
        builder.lore(lore).clean();

        if (on != null && on) {
            builder.glow();
        }
        return builder.build();
    }

    /** Re-renders the panel in place on the viewer's region thread (matches {@code PagedMenu}). */
    private void refresh() {
        build();
        viewer.updateInventory();
    }

    /**
     * @return {@code true} if {@code player} currently has the infinite-duration night-vision
     *         effect applied by {@code /nightvision} (as opposed to a short potion effect).
     */
    private static boolean hasInfiniteNightVision(Player player) {
        final PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        return effect != null && effect.getDuration() == PotionEffect.INFINITE_DURATION;
    }
}
