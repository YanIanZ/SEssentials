package dev.iyanz.sessentials.module.cmiextras;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * Enforces temporary IP bans at the earliest point of the login flow.
 *
 * <p>{@link AsyncPlayerPreLoginEvent} fires on an async connection thread, so this
 * handler must never touch the YAML store or any entity/world API — it only consults
 * the {@link java.util.concurrent.ConcurrentHashMap}-backed snapshot in
 * {@link TempIpBans}, which is safe from any thread. (On Paper 1.21.9 this event lives
 * only under {@code org.bukkit.event.player}; there is no Paper-namespaced variant.)</p>
 */
final class TempIpBanLoginListener implements Listener {

    private final TempIpBans bans;

    /**
     * @param bans the shared temporary-ban registry to consult
     */
    TempIpBanLoginListener(TempIpBans bans) {
        this.bans = bans;
    }

    /**
     * Rejects a connecting address while its temporary ban is unexpired.
     *
     * @param event the async pre-login event
     */
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        Long expiry = bans.activeExpiry(ip);
        if (expiry != null) {
            long remaining = expiry - System.currentTimeMillis();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    Component.text("You are temporarily IP-banned. Time remaining: "
                            + TimeSpans.human(remaining) + "."));
        }
    }
}
