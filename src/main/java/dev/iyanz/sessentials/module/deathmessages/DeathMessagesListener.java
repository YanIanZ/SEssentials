package dev.iyanz.sessentials.module.deathmessages;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.util.Msg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Recolors, or hides, the vanilla death message on every {@link PlayerDeathEvent}.
 *
 * <p>{@link PlayerDeathEvent} fires on the dying player's own region thread, so no
 * scheduler hop is needed here.</p>
 */
final class DeathMessagesListener implements Listener {

    /** Prefix prepended to the (otherwise untouched) vanilla death message. */
    private static final String SKULL_PREFIX = "<#9AA0A6>☠ ";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its live config values
     */
    DeathMessagesListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Hides or recolors the death message per the current {@code deathmessages.*} config. */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("deathmessages.enabled", true)) {
            return;
        }
        if (plugin.getConfig().getBoolean("deathmessages.hide", false)) {
            event.deathMessage(null);
            return;
        }
        if (event.deathMessage() == null) {
            return;
        }
        event.deathMessage(Msg.mm(SKULL_PREFIX).append(event.deathMessage()));
    }
}
