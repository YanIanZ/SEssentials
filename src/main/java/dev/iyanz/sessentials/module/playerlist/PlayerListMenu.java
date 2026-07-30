package dev.iyanz.sessentials.module.playerlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.selib.gui.ItemBuilder;
import dev.iyanz.sessentials.selib.gui.PagedMenu;
import dev.iyanz.sessentials.util.Style;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * SELIB paged chest GUI listing every online player as a textured player-head (clean-room,
 * own layout). Each head's lore shows the target's world, health and game mode, and clicking a
 * head runs a reused SEssentials command as the viewer:
 * <ul>
 *   <li>viewers with {@code sessentials.manage} → {@code /manage <player>} (opens the
 *       player-management GUI);</li>
 *   <li>everyone else → {@code /tpa <player>} (sends a teleport request).</li>
 * </ul>
 *
 * <p>The list auto-paginates via {@link PagedMenu}: the content region is filled with heads and
 * the bottom row carries the previous / page-indicator / next controls.</p>
 *
 * <p><b>Folia-safety.</b> Reading another player's live health / game mode / world off their
 * region thread is unsafe on Folia, so those values are never read cross-region. Instead
 * {@link #scan()} hops to each target's own region thread ({@link Schedulers#entity}) to read a
 * safe snapshot, then hops back to the viewer's region thread to store it in {@link #cache} and
 * re-render. {@link #entries()} (called on the viewer's region thread by {@link #build()}) only
 * ever reads each player's name/UUID and the already-captured cache, so it never touches another
 * player's live entity state. No {@code Bukkit.getScheduler()} is used.</p>
 */
public final class PlayerListMenu extends PagedMenu {

    private static final int ROWS = 6;
    private static final double FULL_HEALTH = 20.0;

    /**
     * Per-target snapshot read on the target's region thread and applied on the viewer's region
     * thread. Keyed by UUID so re-renders (including page changes) rebuild consistently regardless
     * of when each async read returns.
     */
    private record Info(String world, double health, double maxHealth, GameMode mode) {
    }

    /**
     * Snapshots keyed by player UUID. Written and read exclusively on the viewer's region thread
     * (writes hop back there after a target-thread read), so a plain {@link HashMap} is safe.
     */
    private final Map<UUID, Info> cache = new HashMap<>();

    /** Whether the viewer may open the management GUI (decides the click action + hint text). */
    private final boolean canManage;

    /** Set once the view closes so late scan callbacks skip a pointless re-render. */
    private boolean closed;

    public PlayerListMenu(SEssentialsPlugin plugin, Player viewer) {
        super(plugin, viewer, MM.deserialize(Style.title("Online Players")), ROWS);
        this.canManage = viewer.hasPermission("sessentials.manage");
    }

    @Override
    public void open() {
        super.open();
        scan();
    }

    @Override
    protected List<PageEntry> entries() {
        List<PageEntry> list = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            // Only name/UUID (safe cross-region) and the pre-captured snapshot are read here.
            final String name = online.getName();
            list.add(new PageEntry(head(online, cache.get(online.getUniqueId())), event -> click(name)));
        }
        return list;
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        closed = true;
    }

    /**
     * Clicking a head reuses an existing, already-audited command as the viewer (this handler
     * already runs on the viewer's region thread). The reused command handles the "player offline"
     * case itself, so a target that logged off since the list was built degrades gracefully.
     */
    private void click(String name) {
        if (canManage) {
            viewer.performCommand("manage " + name);
        } else {
            viewer.performCommand("tpa " + name);
        }
    }

    /**
     * Reads a safe snapshot of every online player on that player's own region thread, then hops
     * back to the viewer's region thread to store it and re-render. Heads fill in progressively as
     * each region reports back; a target that retires mid-scan simply never reports (harmless).
     */
    private void scan() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            final Player target = online;
            Schedulers.entity(plugin, target, () -> {
                final UUID id = target.getUniqueId();
                final Info info = read(target);
                Schedulers.entity(plugin, viewer, () -> {
                    cache.put(id, info);
                    if (!closed) {
                        refresh();
                    }
                });
            });
        }
    }

    /** Reads live state; must be called on {@code target}'s region thread. */
    private static Info read(Player target) {
        double hp = target.getHealth();
        AttributeInstance max = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = max != null ? max.getValue() : FULL_HEALTH;
        return new Info(target.getWorld().getName(), hp, maxHp, target.getGameMode());
    }

    private ItemStack head(Player target, Info info) {
        List<String> lore = new ArrayList<>();
        if (info == null) {
            lore.add(Style.lore("Loading info..."));
        } else {
            lore.add(Style.labelled("World:", info.world()));
            lore.add(Style.labelled("Health:",
                    String.format(Locale.ROOT, "%.0f / %.0f", info.health(), info.maxHealth())));
            lore.add(Style.labelled("Game Mode:", capitalize(info.mode().name())));
        }
        lore.add("");
        lore.add(Style.hint(canManage ? "Click to manage" : "Click to request teleport"));
        // Player names are word-characters only, so BRAND-wrapping the name literally is injection-safe.
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(Style.button(Style.BRAND, target.getName()))
                .lore(lore)
                .head(target)
                .build();
    }

    private static String capitalize(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
