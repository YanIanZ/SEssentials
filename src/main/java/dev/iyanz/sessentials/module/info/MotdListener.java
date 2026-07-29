package dev.iyanz.sessentials.module.info;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Shows the message of the day (same content as {@code /motd}) to every player as they
 * join, respecting the {@code sessentials.motd} permission.
 */
final class MotdListener implements Listener {

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for config access
     */
    MotdListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Broadcasts the configured (or default) MOTD lines to the joining player. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("sessentials.motd")) {
            return;
        }
        InfoMessages.send(player, plugin, MotdCommand.CONFIG_PATH, MotdCommand.DEFAULT_MOTD);
    }
}
