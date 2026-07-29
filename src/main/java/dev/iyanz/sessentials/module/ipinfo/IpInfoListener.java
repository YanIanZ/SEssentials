package dev.iyanz.sessentials.module.ipinfo;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.store.YamlStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Feeds the {@code "ipinfo"} data store on every join: records the player's current
 * name and appends their connecting IP address to their IP history, de-duplicated
 * and capped to the most recent {@value #MAX_IPS} addresses.
 *
 * <p>{@link PlayerJoinEvent} fires on the joining player's own region thread, so
 * reading {@link Player#getAddress()} here needs no extra Folia scheduler hop.</p>
 */
final class IpInfoListener implements Listener {

    /** Maximum number of distinct IP addresses kept per player. */
    static final int MAX_IPS = 10;

    private static final String KEY_IPS = ".ips";
    private static final String KEY_NAME = ".name";

    private final SEssentialsPlugin plugin;

    /**
     * @param plugin the owning plugin, used for its {@code "ipinfo"} data store
     */
    IpInfoListener(SEssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Appends the joining player's IP (if resolvable) and refreshes their known name. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        YamlStore store = plugin.stores().get("ipinfo");
        String uuid = player.getUniqueId().toString();

        InetSocketAddress address = player.getAddress();
        if (address != null && address.getAddress() != null) {
            String ip = address.getAddress().getHostAddress();
            String ipsPath = uuid + KEY_IPS;

            List<String> ips = new ArrayList<>(store.config().getStringList(ipsPath));
            if (!ips.contains(ip)) {
                ips.add(ip);
            }
            while (ips.size() > MAX_IPS) {
                ips.remove(0);
            }
            store.set(ipsPath, ips);
        }

        store.set(uuid + KEY_NAME, player.getName());
        store.save();
    }
}
