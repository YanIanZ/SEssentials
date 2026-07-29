package dev.iyanz.sessentials.module.mutechat;

import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Enforces the server-wide chat mute on every chat message.
 *
 * <p>Runs at {@link EventPriority#LOW} with {@code ignoreCancelled = true} so it sits
 * just above the per-player mute check (which runs at {@code LOWEST}) yet still skips
 * messages an earlier listener already cancelled. When {@link MuteChatState global
 * chat is muted}, any sender lacking {@code sessentials.mutechat.bypass} has their
 * message cancelled and is told, once, that chat is muted; staff with the bypass
 * permission chat normally.</p>
 */
public final class MuteChatListener implements Listener {

    /**
     * Cancels chat while global mute is active, unless the sender may bypass it.
     *
     * @param event the async chat event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!MuteChatState.muted()) {
            return; // chat is open
        }
        Player player = event.getPlayer();
        if (player.hasPermission("sessentials.mutechat.bypass")) {
            return; // staff bypass
        }
        event.setCancelled(true);
        Msg.err(player, "Chat is currently muted.");
    }
}
