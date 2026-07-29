package dev.iyanz.sessentials.module.teleport;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;
import org.bukkit.entity.Player;

/**
 * Teleportation suite: direct teleports ({@code /tp}, {@code /tphere}, {@code /tpall},
 * {@code /tppos}, {@code /top}), consent-based requests ({@code /tpa}, {@code /tpahere},
 * {@code /tpaccept}/{@code /tpyes}, {@code /tpdeny}/{@code /tpno}), teleport/death
 * history ({@code /back}/{@code /return}) and the server spawn ({@code /spawn},
 * {@code /setspawn}).
 *
 * <p>Every teleport goes through {@link Player#teleportAsync}, and any read of another
 * player's location happens on that player's own region thread (see
 * {@link Teleports}), so the whole module is Folia-safe.</p>
 */
public final class TeleportModule implements EssModule {

    private final TeleportRequests requests = new TeleportRequests();
    private final TeleportHistory history = new TeleportHistory();

    @Override
    public String name() {
        return "teleport";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new TeleportHistoryListener(history), plugin);

        TeleportCommands.register(plugin);
        TeleportRequestCommands.register(plugin, requests);
        BackCommand.register(plugin, history);
        SpawnCommands.register(plugin);
    }
}
