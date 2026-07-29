package dev.iyanz.sessentials.module.commandspy;

import java.util.Set;
import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Mirrors every command a player runs to the online command-spy watchers.
 *
 * <p>Listens at {@link EventPriority#MONITOR} with {@code ignoreCancelled = true}, so a
 * command another plugin blocked is never shown &mdash; watchers only see commands that
 * actually go through. A player holding {@code sessentials.commandspy.exempt} (staff and
 * admins whose commands should stay private) is never mirrored.</p>
 *
 * <p>The raw command text is wrapped with {@link Component#text(String)} rather than
 * parsed as MiniMessage, so nothing a player types into a command can inject colour
 * tags, click events or similar markup into a watcher's chat &mdash; only the surrounding
 * {@code [cspy]} label is coloured.</p>
 *
 * <p>Folia-safe: {@link PlayerCommandPreprocessEvent} fires on the sending player's own
 * region thread, and each watcher is messaged on their own region thread via
 * {@link Schedulers#entity(org.bukkit.plugin.Plugin, org.bukkit.entity.Entity, Runnable)}.</p>
 */
final class CommandSpyListener implements Listener {

    /** Permission a watcher must still hold for a command to be mirrored to them. */
    private static final String SPY_PERMISSION = "sessentials.commandspy";

    /** Permission that exempts the running player's commands from being shown. */
    private static final String EXEMPT_PERMISSION = "sessentials.commandspy.exempt";

    private final SEssentialsPlugin plugin;
    private final Set<UUID> spies;

    /**
     * @param plugin the owning plugin, used to schedule per-watcher region-thread sends
     * @param spies  the shared set of players currently watching commands
     */
    CommandSpyListener(SEssentialsPlugin plugin, Set<UUID> spies) {
        this.plugin = plugin;
        this.spies = spies;
    }

    /** Sends a spy-formatted copy of the command to every eligible online watcher. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (spies.isEmpty()) {
            return;
        }
        Player sender = event.getPlayer();
        if (sender.hasPermission(EXEMPT_PERMISSION)) {
            return;
        }

        UUID senderId = sender.getUniqueId();
        Component line = Msg.mm("<#9AA0A6>[cspy] <#F5F5F5>" + sender.getName() + "<#9AA0A6>: ")
                .append(Component.text(event.getMessage(), NamedTextColor.WHITE));

        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID id = online.getUniqueId();
            if (id.equals(senderId) || !spies.contains(id) || !online.hasPermission(SPY_PERMISSION)) {
                continue;
            }
            Schedulers.entity(plugin, online, () -> online.sendMessage(line));
        }
    }
}
