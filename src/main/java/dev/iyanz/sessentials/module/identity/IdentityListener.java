package dev.iyanz.sessentials.module.identity;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Keeps the {@code "identity"} data store up to date across joins/quits: records
 * each player's current name, stamps their last-seen time on quit, and re-applies
 * their persisted nickname on join. Also clears transient AFK state on disconnect.
 */
public final class IdentityListener implements Listener {

    private static final String KEY_NAME = ".name";
    private static final String KEY_LAST = ".last";

    private final SEssentialsPlugin plugin;
    private final NickService nickService;
    private final AfkService afkService;

    /**
     * @param plugin      the owning plugin, used for its data store
     * @param nickService the service used to re-apply nicknames on join
     * @param afkService  the AFK-state tracker to clear on quit
     */
    public IdentityListener(SEssentialsPlugin plugin, NickService nickService, AfkService afkService) {
        this.plugin = plugin;
        this.nickService = nickService;
        this.afkService = afkService;
    }

    /** Records the player's name and re-applies their persisted nickname. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        YamlStore store = plugin.stores().get("identity");
        store.set(player.getUniqueId() + KEY_NAME, player.getName());
        store.save();
        nickService.reapply(player);
    }

    /** Stamps the last-seen time (and name), and clears transient AFK state. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        YamlStore store = plugin.stores().get("identity");
        store.set(player.getUniqueId() + KEY_LAST, Long.toString(System.currentTimeMillis()));
        store.set(player.getUniqueId() + KEY_NAME, player.getName());
        store.save();
        afkService.clear(player.getUniqueId());
    }
}
