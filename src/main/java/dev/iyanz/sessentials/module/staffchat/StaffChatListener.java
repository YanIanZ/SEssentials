package dev.iyanz.sessentials.module.staffchat;

import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Diverts the normal chat of any player who has staff-chat mode enabled into the staff
 * channel, and forgets a player's staff-chat-mode flag when they leave.
 *
 * <p>The chat handler runs at {@link EventPriority#LOW} with {@code ignoreCancelled =
 * true}: this places it right after the moderation module's mute check (at {@link
 * EventPriority#LOWEST}), so a muted player cannot use staff-chat mode to bypass their
 * mute, while still running before the plugin's later chat-colour/format listeners that
 * would only ever touch the now-cancelled public message.</p>
 *
 * <p>The message is flattened to plain text via {@link PlainTextComponentSerializer}
 * before being handed to {@link StaffChatService#broadcast}, so the sender's own text is
 * always rendered verbatim and can never inject markup into the staff channel.</p>
 */
public final class StaffChatListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final StaffChatService service;

    /**
     * @param plugin  the owning plugin, forwarded to the service for its schedulers
     * @param service the shared staff-chat state and delivery service
     */
    public StaffChatListener(SEssentialsPlugin plugin, StaffChatService service) {
        this.plugin = plugin;
        this.service = service;
    }

    /** Cancels and re-routes to the staff channel any chat from a player in staff-chat mode. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!service.inChatMode(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        service.broadcast(plugin, player.getName(), plain);
    }

    /** Drops the leaving player's staff-chat-mode flag so the tracking set never leaks. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.clear(event.getPlayer().getUniqueId());
    }
}
