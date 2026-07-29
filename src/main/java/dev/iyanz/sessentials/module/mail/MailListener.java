package dev.iyanz.sessentials.module.mail;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Notifies a player of any unread mail as soon as they join. The join event fires
 * on the joining player's own region thread, so the notification is sent directly
 * with no scheduler hop required.
 */
final class MailListener implements Listener {

    private final SEssentialsPlugin plugin;

    /** @param plugin the owning plugin, used to reach the mail data store */
    MailListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Reports the unread mail count, if any, on join. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        YamlStore store = MailService.store(plugin);
        int unread = MailService.unreadCount(store, player.getUniqueId());
        if (unread > 0) {
            Msg.info(player, "You have " + unread + " unread mail message" + (unread == 1 ? "" : "s")
                    + ". Use /mail read to view it.");
        }
    }
}
