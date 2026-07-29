package dev.iyanz.sessentials.module.lockdown;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * Enforces the server-wide lockdown at the earliest point of a connection.
 *
 * <p>Handles {@link AsyncPlayerPreLoginEvent}, which fires off any region thread
 * before the {@code Player} is constructed. Because there is no player yet, no
 * permission check is possible (or intended) here: while {@link LockdownState the
 * lockdown is active} <em>every</em> brand-new login is refused with
 * {@link AsyncPlayerPreLoginEvent.Result#KICK_OTHER} and the current lockdown reason.
 * Staff already online (who hold {@code sessentials.lockdown.bypass}) are untouched
 * and can lift the lockdown; the block applies only to fresh connection attempts.</p>
 *
 * <p>The handler only reads the {@code volatile} lockdown state, so it is safe to run
 * on the async pre-login thread.</p>
 */
public final class LockdownListener implements Listener {

    /**
     * Refuses new logins while the server is locked down.
     *
     * @param event the async pre-login event
     */
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!LockdownState.active()) {
            return; // server is open
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("Server is locked: " + LockdownState.reason()));
    }
}
