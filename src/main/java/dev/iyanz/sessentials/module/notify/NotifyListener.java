package dev.iyanz.sessentials.module.notify;

import dev.iyanz.sessentials.SEssentialsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Sends a subtle staff-only "player joined/left" line to every online holder of
 * {@link NotifyModule#RECEIVE_PERMISSION}, gated by the {@code notify.join-leave}
 * config key (default {@code false} — off unless a server operator opts in).
 */
final class NotifyListener implements Listener {

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its config and the notify broadcast
     */
    NotifyListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Announces the joining player, if the join/leave feed is enabled. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        announce(event.getPlayer(), "joined");
    }

    /** Announces the departing player, if the join/leave feed is enabled. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        announce(event.getPlayer(), "left");
    }

    private void announce(Player player, String verb) {
        if (!plugin.getConfig().getBoolean("notify.join-leave", false)) {
            return;
        }
        Component message = NotifyModule.label()
                .append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Component.text(" " + verb, NamedTextColor.GRAY));
        NotifyModule.broadcast(plugin, message);
    }
}
