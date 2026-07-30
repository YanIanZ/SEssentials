package dev.iyanz.sessentials.module.playerstate;

import java.util.UUID;

import dev.iyanz.sessentials.SEssentialsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * On quit, persists the player's god-mode flag when {@code god.persist} is enabled so god
 * mode survives a relog (like CMI), then clears the in-memory {@link GodService} entry to
 * keep the runtime registry bounded.
 *
 * <p>The event fires on the quitting player's own region thread; the persistence store is
 * monitor-guarded and the registry set is concurrent, so no scheduler hop is required.</p>
 *
 * @see GodService
 * @see GodPersistence
 * @see GodJoinListener
 */
final class GodQuitListener implements Listener {

    private final SEssentialsPlugin plugin;
    private final GodService godService;

    /**
     * @param plugin     owning plugin, used to read config and the persistence store
     * @param godService the shared god-mode registry to persist and clean up on quit
     */
    GodQuitListener(SEssentialsPlugin plugin, GodService godService) {
        this.plugin = plugin;
        this.godService = godService;
    }

    /** Persists the god flag (if enabled and set), then removes the player from the registry. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (GodPersistence.enabled(plugin) && godService.isGod(id)) {
            GodPersistence.store(plugin, id);
        }
        godService.remove(id);
    }
}
