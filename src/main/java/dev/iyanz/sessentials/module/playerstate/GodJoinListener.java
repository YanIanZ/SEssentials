package dev.iyanz.sessentials.module.playerstate;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Restores god mode for a joining player whose flag was persisted across their previous
 * session (when {@code god.persist} is enabled): re-adds them to the in-memory
 * {@link GodService} and marks them invulnerable, matching CMI's persistent god mode.
 *
 * <p>{@link PlayerJoinEvent} fires on the joining player's own region thread, so mutating
 * the player entity ({@link Player#setInvulnerable(boolean)}) here needs no extra Folia
 * scheduler hop.</p>
 *
 * @see GodService
 * @see GodPersistence
 * @see GodQuitListener
 */
final class GodJoinListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final GodService godService;

    /**
     * @param plugin     owning plugin, used to read config and the persistence store
     * @param godService the shared god-mode registry to restore into
     */
    GodJoinListener(SEssentialsPlugin plugin, GodService godService) {
        this.plugin = plugin;
        this.godService = godService;
    }

    /** Re-enables god mode if the joining player has a persisted flag. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!GodPersistence.enabled(plugin) || !GodPersistence.isStored(plugin, player.getUniqueId())) {
            return;
        }
        godService.add(player.getUniqueId());
        player.setInvulnerable(true);
    }
}
