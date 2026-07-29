package dev.iyanz.sessentials.module.localchat;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Restricts each chat message to nearby players by pruning the event's viewer set, with a
 * {@code !} prefix as an escape hatch back to server-wide "global" chat.
 *
 * <p>When {@code local-chat.enabled} is set, a normal message (one that does not begin
 * with {@code !}) is delivered only to players within {@code local-chat.radius} blocks of
 * the sender in the <em>same</em> world. This is achieved without cancelling the event:
 * every out-of-range {@link Player} audience is removed from {@link
 * AsyncChatEvent#viewers()}, leaving the sender, in-range players and every non-player
 * audience (notably the console, so staff can still monitor) untouched. Both the enabled
 * flag and the radius are read fresh on each message, so {@code /localchat reload} takes
 * effect immediately.</p>
 *
 * <p>A message that begins with the {@code !} global prefix is delivered to everyone: the
 * marker is stripped and the remainder is re-injected as a literal {@link Component} built
 * from plain text via {@link PlainTextComponentSerializer} — <strong>never</strong>
 * re-parsed as MiniMessage — so a player can never smuggle markup, colours or click/hover
 * events through the global channel. Players holding {@code sessentials.localchat.bypass}
 * always chat globally and are never subject to the radius filter.</p>
 *
 * <p>Runs at {@link EventPriority#NORMAL} with {@code ignoreCancelled = true}: this sits
 * after the moderation module's mute check ({@link EventPriority#LOWEST}) and the
 * staff-chat / chat-filter listeners ({@link EventPriority#LOW}), so a muted or diverted
 * message is never processed here.</p>
 *
 * <p>The chat handler runs on the asynchronous chat thread, where calling {@link
 * Player#getLocation()} on the sender or any viewer is illegal under Folia (it throws an
 * entity thread-check {@link IllegalStateException}, which would abort the viewer prune
 * and leak the message server-wide) and racy in general. To avoid touching entity/world
 * API off-thread, each player's last-known position is cached in a {@link
 * ConcurrentHashMap} that is updated only on that player's own region thread — on move,
 * teleport and join — and evicted on quit. The async handler computes the in-range set
 * purely from that cache and never calls {@code getLocation()}.</p>
 */
public final class LocalChatListener implements Listener {

    /** Leading marker that forces a message to be delivered server-wide. */
    private static final String GLOBAL_PREFIX = "!";

    private final SEssentialsPlugin plugin;

    /**
     * Last-known position of each online player, written only on the owning player's region
     * thread (move/teleport/join) and read by the async chat handler. Never holds a live
     * Bukkit {@link Location}, so reading it off-thread triggers no entity thread-check.
     */
    private final Map<UUID, Pos> positions = new ConcurrentHashMap<>();

    /**
     * @param plugin the owning plugin, used for its live {@code local-chat.*} config
     */
    public LocalChatListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Prunes distant viewers for a local message, or strips the {@code !} prefix for a global one. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("local-chat.enabled", false)) {
            return;
        }

        Player sender = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (plain.startsWith(GLOBAL_PREFIX)) {
            // Global override: strip the marker, re-inject the rest as plain text only,
            // and leave the viewer set untouched so everyone receives it.
            String global = plain.substring(GLOBAL_PREFIX.length());
            event.message(Component.text(global));
            return;
        }

        if (sender.hasPermission("sessentials.localchat.bypass")) {
            return; // bypassing players always chat globally
        }

        restrictToRadius(event, sender);
    }

    /**
     * Removes from the event's viewer set every {@link Player} audience that is not within
     * the configured radius of the sender in the same world. Console and any other
     * non-player audience are preserved; the sender (distance zero) always survives.
     *
     * <p>Distances are computed entirely from the {@link #positions} cache, never from a
     * live {@link Location}, so this is safe on the async chat thread.</p>
     *
     * @param event  the chat event whose viewers are being pruned
     * @param sender the player who sent the message
     */
    private void restrictToRadius(AsyncChatEvent event, Player sender) {
        Pos origin = positions.get(sender.getUniqueId());
        if (origin == null) {
            return; // sender position not yet cached; leave delivery untouched rather than guess
        }
        double radius = plugin.getConfig().getInt("local-chat.radius", 100);
        double radiusSquared = radius * radius;

        // Iterate a snapshot copy so we can mutate the live viewer set while looping.
        for (Audience viewer : new ArrayList<>(event.viewers())) {
            if (!(viewer instanceof Player recipient)) {
                continue; // keep the console and every other non-player audience
            }
            Pos pos = positions.get(recipient.getUniqueId());
            if (pos == null || !origin.sameWorld(pos) || origin.distanceSquared(pos) > radiusSquared) {
                event.viewers().remove(viewer);
            }
        }
    }

    /** Caches the joining player's position on their own region thread. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        positions.put(player.getUniqueId(), Pos.of(player.getLocation()));
    }

    /** Refreshes the cache when a player moves into a new block (ignores look-only moves). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        positions.put(event.getPlayer().getUniqueId(), Pos.of(event.getTo()));
    }

    /** Refreshes the cache on teleport, including cross-world jumps a move event never reports. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to != null) {
            positions.put(event.getPlayer().getUniqueId(), Pos.of(to));
        }
    }

    /** Evicts the player's cached position on quit. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        positions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * An immutable, thread-safe snapshot of a player's position: the world's UID plus block
     * coordinates. Deliberately does not retain a live {@link Location}/{@link World}, so it
     * can be compared from any thread without an entity/world thread-check.
     */
    private record Pos(UUID world, double x, double y, double z) {

        static Pos of(Location loc) {
            World w = loc.getWorld();
            return new Pos(w == null ? null : w.getUID(), loc.getX(), loc.getY(), loc.getZ());
        }

        boolean sameWorld(Pos other) {
            return world != null && world.equals(other.world);
        }

        double distanceSquared(Pos other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
