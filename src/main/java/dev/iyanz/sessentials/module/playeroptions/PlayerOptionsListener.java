package dev.iyanz.sessentials.module.playeroptions;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Restores flight on join for players who have enabled {@code /flytoggle}'s
 * "keep flight on join" preference and still hold {@code sessentials.fly}.
 *
 * <p>{@link PlayerJoinEvent} fires on the joining player's own region thread, so no
 * scheduler hop is needed to touch their entity state here (Folia-safe).</p>
 */
final class PlayerOptionsListener implements Listener {

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used to read the persisted preference
     */
    PlayerOptionsListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Re-enables {@link Player#setAllowFlight} for eligible players as they join. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("sessentials.fly")) {
            return;
        }
        if (!PlayerOptionsModule.flyKeepEnabled(plugin, player.getUniqueId())) {
            return;
        }
        player.setAllowFlight(true);
    }
}
