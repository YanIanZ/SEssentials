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
 *
 * <p>The player-initiated teleports ({@code /tp} self-move, {@code /tpaccept},
 * {@code /back}, {@code /spawn}) share a post-teleport cooldown gate
 * ({@link TeleportCooldown}, configured by {@code teleport.cooldown-seconds}).</p>
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
        TeleportCooldown cooldown = new TeleportCooldown(plugin);
        plugin.getServer().getPluginManager().registerEvents(new TeleportHistoryListener(history, requests, cooldown), plugin);

        TeleportCommands.register(plugin, cooldown);
        TeleportRequestCommands.register(plugin, requests, cooldown);
        BackCommand.register(plugin, history, cooldown);
        SpawnCommands.register(plugin, cooldown);
    }
}
