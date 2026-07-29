package dev.iyanz.sessentials.module.localchat;

import java.util.ArrayList;

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
 * message is never processed here. Only player locations and the viewer set are touched,
 * so the handler is safe to run on the asynchronous chat thread under Folia.</p>
 */
public final class LocalChatListener implements Listener {

    /** Leading marker that forces a message to be delivered server-wide. */
    private static final String GLOBAL_PREFIX = "!";

    private final SEssentialsPlugin plugin;

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
     * @param event  the chat event whose viewers are being pruned
     * @param sender the player who sent the message
     */
    private void restrictToRadius(AsyncChatEvent event, Player sender) {
        Location origin = sender.getLocation();
        World world = origin.getWorld();
        double radius = plugin.getConfig().getInt("local-chat.radius", 100);
        double radiusSquared = radius * radius;

        // Iterate a snapshot copy so we can mutate the live viewer set while looping.
        for (Audience viewer : new ArrayList<>(event.viewers())) {
            if (!(viewer instanceof Player recipient)) {
                continue; // keep the console and every other non-player audience
            }
            Location location = recipient.getLocation();
            if (!world.equals(location.getWorld()) || origin.distanceSquared(location) > radiusSquared) {
                event.viewers().remove(viewer);
            }
        }
    }
}
