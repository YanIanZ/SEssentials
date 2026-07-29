package dev.iyanz.sessentials.module.ipban;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * Enforces IP bans at the earliest possible point in the login flow.
 *
 * <p>{@link AsyncPlayerPreLoginEvent} fires off-region on an async connection thread,
 * so this handler must never touch the YAML store or any entity/world API. It only
 * reads the {@link ConcurrentHashMap}-backed snapshot held by {@link BannedIps}, which
 * is safe from any thread.</p>
 *
 * <p>Note: on Paper 1.21.9 the Adventure {@link Component}-aware {@code disallow} and
 * {@link AsyncPlayerPreLoginEvent#getAddress() getAddress} methods live on the standard
 * {@code org.bukkit.event.player.AsyncPlayerPreLoginEvent}; there is no separate
 * {@code io.papermc.paper.event.player} variant in this API version.</p>
 */
final class IpBanLoginListener implements Listener {

    private final BannedIps banned;

    /**
     * @param banned the shared banned-IP registry to consult
     */
    IpBanLoginListener(BannedIps banned) {
        this.banned = banned;
    }

    /**
     * Rejects a connecting address if it is banned.
     *
     * @param event the async pre-login event
     */
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        if (banned.isBanned(ip)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text("You are IP-banned."));
        }
    }
}
