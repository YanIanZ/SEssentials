package dev.iyanz.sessentials.module.managegui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.selib.gui.Buttons;
import dev.iyanz.sessentials.selib.gui.ConfirmMenu;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.Menu;
import dev.iyanz.sessentials.util.Msg;
import dev.iyanz.sessentials.util.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB chest GUI for managing a single online player (clean-room, CMI-style purpose). The menu
 * shows a textured player-head of the target with live health / game-mode / world lore, plus a
 * grid of action buttons.
 *
 * <p><b>Reuse, not re-implementation.</b> Every button dispatches an existing, already-audited
 * SEssentials command instead of duplicating its effect:</p>
 * <ul>
 *   <li>Target-affecting actions (heal, feed, extinguish, kill, kick, ban, mute/unmute, freeze,
 *       game-mode) are dispatched as the <em>console</em>, on the global region scheduler, so the
 *       reused command runs from the correct thread and with full permissions.</li>
 *   <li>Viewer-context actions ({@code /tp}, {@code /invsee}, {@code /endersee}) are dispatched as
 *       the <em>viewer</em> on the viewer's region thread, exactly as if the viewer had typed them
 *       (these commands are player-only and act on the sender).</li>
 *   <li>Vanish is the one command with no target form ({@code /vanish} toggles the sender). It is
 *       dispatched as the <em>target</em> on the target's region thread, which reuses the audited
 *       self-toggle; note this therefore requires the target to hold {@code sessentials.vanish}.</li>
 * </ul>
 *
 * <p>Folia-safe: the base {@link Menu} opens on the viewer's region thread and routes clicks there;
 * the head refresh reads the target on the target's region thread and hops back to the viewer to
 * update the icon; world-touching console dispatches use the global region scheduler.</p>
 */
public final class ManageMenu extends Menu {

    private static final int ROWS = 6;

    private static final int SLOT_HEAD = 4;
    // Row 2 — utility & inspection (non-punitive).
    private static final int SLOT_HEAL = 10;
    private static final int SLOT_FEED = 11;
    private static final int SLOT_EXTINGUISH = 12;
    private static final int SLOT_GAMEMODE = 13;
    private static final int SLOT_TELEPORT = 14;
    private static final int SLOT_INVSEE = 15;
    private static final int SLOT_ENDERCHEST = 16;
    // Row 3 — moderation & punishment.
    private static final int SLOT_VANISH = 19;
    private static final int SLOT_FREEZE = 20;
    private static final int SLOT_MUTE = 21;
    private static final int SLOT_KICK = 23;
    private static final int SLOT_BAN = 24;
    private static final int SLOT_KILL = 25;
    private static final int SLOT_CLOSE = 49;

    private static final int FULL_HEALTH = 20;

    private final String targetName;

    /**
     * Last known game mode of the target, captured by the head refresh on the target's region
     * thread. Used to compute the next mode for the game-mode cycle button without reading the
     * target cross-region on click.
     */
    private GameMode cachedMode;

    /**
     * @param plugin the owning plugin
     * @param viewer the staff member opening the menu
     * @param target the online player being managed (used for the initial head skin and name)
     */
    public ManageMenu(SEssentialsPlugin plugin, Player viewer, Player target) {
        super(plugin, viewer, title(target.getName()), ROWS);
        this.targetName = target.getName();
        // Live game mode is read cross-region-safely by the head refresh (on the target's region
        // thread); until then it is unknown. The head skin only stores the target's profile, which
        // is safe to set from any thread.
        set(SLOT_HEAD, head(target), event -> refreshHead());
    }

    private static Component title(String name) {
        // Player names are word-characters only, so appending literally carries no MiniMessage
        // injection risk; kept out of the deserialized fragment to be safe regardless.
        return MM.deserialize(Style.title("Manage") + " " + Style.VALUE + name);
    }

    @Override
    protected void build() {
        border(Buttons.filler());

        set(SLOT_HEAL, button(Material.GOLDEN_APPLE, Style.DEPOSIT, "Heal", "Restore full health & hunger"),
                e -> console("heal " + targetName));
        set(SLOT_FEED, button(Material.COOKED_BEEF, Style.DEPOSIT, "Feed", "Refill the hunger bar"),
                e -> console("feed " + targetName));
        set(SLOT_EXTINGUISH, button(Material.WATER_BUCKET, Style.INFO, "Extinguish", "Put out fire"),
                e -> console("extinguish " + targetName));
        renderGamemode();
        set(SLOT_TELEPORT, button(Material.ENDER_PEARL, Style.INFO, "Teleport To", "Teleport yourself to them"),
                e -> viewerTeleport());
        set(SLOT_INVSEE, button(Material.CHEST, Style.INFO, "Inventory", "View & edit their inventory"),
                e -> viewer("invsee " + targetName));
        set(SLOT_ENDERCHEST, button(Material.ENDER_CHEST, Style.INFO, "Ender Chest", "View & edit their ender chest"),
                e -> viewer("endersee " + targetName));

        set(SLOT_VANISH, button(Material.GRAY_DYE, Style.UPGRADE, "Vanish",
                "Toggle their vanish", "Requires them to have vanish access"), e -> vanish());
        set(SLOT_FREEZE, button(Material.PACKED_ICE, Style.UPGRADE, "Freeze", "Toggle freezing them in place"),
                e -> console("freeze " + targetName));
        set(SLOT_MUTE, muteButton(), this::mute);
        set(SLOT_KICK, button(Material.IRON_BOOTS, Style.WITHDRAW, "Kick", "Disconnect them", "Asks for confirmation"),
                e -> confirmKick());
        set(SLOT_BAN, button(Material.BARRIER, Style.DANGER, "Ban", "Ban them", "Asks for confirmation"),
                e -> confirmBan());
        set(SLOT_KILL, button(Material.NETHERITE_SWORD, Style.DANGER, "Kill", "Instantly kill them"),
                e -> console("kill " + targetName));

        set(SLOT_CLOSE, Buttons.close(), e -> viewer.closeInventory());

        refreshHead();
    }

    // --- actions -------------------------------------------------------------

    /**
     * Dispatches a target-affecting command as the console, on the global region scheduler so the
     * reused command executes from the correct Folia thread (it then schedules any per-target work
     * on the target's own region thread itself).
     */
    private void console(String command) {
        if (offline()) {
            return;
        }
        CommandSender console = Bukkit.getConsoleSender();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(console, command));
        refreshHead();
    }

    /** Dispatches a player-only command as the viewer (their click handler already runs on their thread). */
    private void viewer(String command) {
        if (offline()) {
            return;
        }
        Bukkit.dispatchCommand(viewer, command);
    }

    /** Teleport-to: closes this menu, then runs {@code /tp <target>} as the viewer. */
    private void viewerTeleport() {
        if (offline()) {
            return;
        }
        viewer.closeInventory();
        Bukkit.dispatchCommand(viewer, "tp " + targetName);
    }

    /** Mute (left-click) / Unmute (right-click) via the audited moderation commands. */
    private void mute(InventoryClickEvent event) {
        console((event.isRightClick() ? "unmute " : "mute ") + targetName);
    }

    /**
     * Toggles the target's vanish by dispatching the self-only {@code /vanish} on the target's
     * region thread as the target (there is no {@code /vanish <player>} form to reuse otherwise).
     */
    private void vanish() {
        Player t = Bukkit.getPlayerExact(targetName);
        if (t == null) {
            notifyOffline();
            return;
        }
        Schedulers.entity(plugin, t, () -> Bukkit.dispatchCommand(t, "vanish"));
    }

    /** Cycles the target's game mode via {@code /gamemode <mode> <target>} dispatched as console. */
    private void cycleGamemode() {
        if (offline()) {
            return;
        }
        GameMode next = nextMode(cachedMode == null ? GameMode.SURVIVAL : cachedMode);
        cachedMode = next;
        renderGamemode();
        console("gamemode " + next.name().toLowerCase(Locale.ROOT) + " " + targetName);
    }

    private void confirmKick() {
        if (offline()) {
            return;
        }
        ItemStack prompt = button(Material.IRON_BOOTS, Style.WITHDRAW, "Kick " + targetName, "Disconnect this player");
        new ConfirmMenu(plugin, viewer, MM.deserialize(Style.title("Confirm Kick")), prompt,
                () -> console("kick " + targetName), this::reopen).open();
    }

    private void confirmBan() {
        if (offline()) {
            return;
        }
        ItemStack prompt = button(Material.BARRIER, Style.DANGER, "Ban " + targetName, "Permanently ban this player");
        new ConfirmMenu(plugin, viewer, MM.deserialize(Style.title("Confirm Ban")), prompt,
                () -> console("ban " + targetName), this::reopen).open();
    }

    /** Re-opens a fresh management menu for the same target (used when a confirm is cancelled). */
    private void reopen() {
        Player t = Bukkit.getPlayerExact(targetName);
        if (t != null) {
            new ManageMenu(plugin, viewer, t).open();
        }
    }

    // --- head / icons --------------------------------------------------------

    /**
     * Reads the target's live health, game mode and world on the target's region thread, then hops
     * back to the viewer's region thread to update the head icon and the cached game mode.
     */
    private void refreshHead() {
        Player t = Bukkit.getPlayerExact(targetName);
        if (t == null) {
            return;
        }
        Schedulers.entity(plugin, t, () -> {
            double hp = t.getHealth();
            AttributeInstance max = t.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = max != null ? max.getValue() : FULL_HEALTH;
            GameMode mode = t.getGameMode();
            String world = t.getWorld().getName();
            Schedulers.entity(plugin, viewer, () -> {
                cachedMode = mode;
                set(SLOT_HEAD, head(t, hp, maxHp, mode, world), event -> refreshHead());
                renderGamemode();
            });
        });
    }

    private void renderGamemode() {
        String current = cachedMode == null ? "Unknown" : capitalize(cachedMode.name());
        set(SLOT_GAMEMODE, new ItemBuilder(Material.GRASS_BLOCK)
                .name(Style.button(Style.INFO, "Game Mode"))
                .lore(Style.labelled("Current:", current), Style.hint("Click to cycle"))
                .clean()
                .build(), e -> cycleGamemode());
    }

    /** Initial head (no live stats yet). */
    private ItemStack head(Player target) {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(Style.button(Style.BRAND, target.getName()))
                .lore(Style.hint("Click to refresh info"))
                .head(target)
                .build();
    }

    /** Head with live stats. */
    private ItemStack head(Player target, double hp, double maxHp, GameMode mode, String world) {
        List<String> lore = new ArrayList<>();
        lore.add(Style.labelled("Health:",
                String.format(Locale.ROOT, "%.0f / %.0f", hp, maxHp)));
        lore.add(Style.labelled("Game Mode:", capitalize(mode.name())));
        lore.add(Style.labelled("World:", world));
        lore.add("");
        lore.add(Style.hint("Click to refresh info"));
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(Style.button(Style.BRAND, target.getName()))
                .lore(lore)
                .head(target)
                .build();
    }

    private ItemStack muteButton() {
        return button(Material.NAME_TAG, Style.WITHDRAW, "Mute", "Left-click: mute", "Right-click: unmute");
    }

    private static ItemStack button(Material material, String color, String label, String... loreLines) {
        List<String> lore = new ArrayList<>(loreLines.length);
        for (int i = 0; i < loreLines.length; i++) {
            // First line is a plain description; any extra lines are click hints.
            lore.add(i == 0 ? Style.lore(loreLines[i]) : Style.hint(loreLines[i]));
        }
        return new ItemBuilder(material)
                .name(Style.button(color, label))
                .lore(lore)
                .clean()
                .build();
    }

    // --- helpers -------------------------------------------------------------

    /** @return {@code true} (after notifying + closing) if the target is no longer online. */
    private boolean offline() {
        if (Bukkit.getPlayerExact(targetName) == null) {
            notifyOffline();
            return true;
        }
        return false;
    }

    private void notifyOffline() {
        Msg.err(viewer, targetName + " is no longer online.");
        viewer.closeInventory();
    }

    private static GameMode nextMode(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> GameMode.CREATIVE;
            case CREATIVE -> GameMode.ADVENTURE;
            case ADVENTURE -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }

    private static String capitalize(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
