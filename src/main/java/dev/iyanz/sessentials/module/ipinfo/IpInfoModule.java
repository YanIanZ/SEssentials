package dev.iyanz.sessentials.module.ipinfo;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Staff IP-history tooling: records each player's connecting IP addresses on join,
 * and exposes {@code /ipinfo <player>} (recorded IP history) and
 * {@code /ipmatch <player>} (alt-account detection via shared IPs).
 *
 * <p>Data is persisted in the {@code "ipinfo"} data store, keyed per player UUID:
 * {@code "<uuid>.name"} (last known name) and {@code "<uuid>.ips"} (a de-duplicated
 * list of the last few IPs seen for that player, maintained by
 * {@link IpInfoListener}).</p>
 */
public final class IpInfoModule implements EssModule {

    @Override
    public String name() {
        return "ipinfo";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new IpInfoListener(plugin), plugin);
        IpInfoCommands.register(plugin);
    }
}
