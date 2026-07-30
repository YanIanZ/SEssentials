package dev.iyanz.sessentials.module.staffextras;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Event side of the staff-extras module.
 *
 * <ul>
 *   <li><b>Shadow mute</b> — at {@link EventPriority#LOWEST} a shadow-muted sender's
 *       chat message is cancelled before any other plugin sees it, then echoed back
 *       to the sender alone as a plain {@code &lt;name&gt; message} line, so the
 *       player keeps seeing their own chat and does not realise they are muted.</li>
 *   <li><b>No-target</b> — mobs that try to target a flagged player have the
 *       targeting event cancelled.</li>
 *   <li><b>Cleanup</b> — every trace of a player is dropped when they quit.</li>
 * </ul>
 *
 * <p>Folia-safe: {@link AsyncChatEvent} fires off-region and this handler only reads
 * a concurrent set, cancels the event and messages the sender (thread-safe Adventure
 * audience). The echoed line is built with {@link Component#text(String)} — the
 * player's raw text is never run through MiniMessage.</p>
 */
public final class StaffExtrasListener implements Listener {

    /** Shared module state written by the commands and read here. */
    private final StaffExtrasState state;

    /**
     * @param state the shared holder the module's commands write and this listener reads
     */
    StaffExtrasListener(StaffExtrasState state) {
        this.state = state;
    }

    /**
     * Silently drops a shadow-muted player's chat for everyone else while echoing it
     * back to the sender as their own chat line.
     *
     * @param event the asynchronous chat event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        if (!state.isShadowMuted(sender.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        sender.sendMessage(Component.text("<" + sender.getName() + "> ").append(event.message()));
    }

    /**
     * Stops mobs from targeting players flagged with {@code /notarget}.
     *
     * @param event the mob targeting event
     */
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player target && state.isNoTarget(target.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Clears a departing player's flags and patrol position.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        state.forget(event.getPlayer().getUniqueId());
    }
}
