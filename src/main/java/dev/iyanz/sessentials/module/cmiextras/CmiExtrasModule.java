package dev.iyanz.sessentials.module.cmiextras;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Grab-bag of small admin utilities rounding out the command set:
 * <ul>
 *   <li>{@code /oplist} — list server operators (online and offline).</li>
 *   <li>{@code /colors} — rendered reference of legacy {@code &}-codes and MiniMessage.</li>
 *   <li>{@code /tpo <player>} — admin teleport override (ignores any teleport toggle).</li>
 *   <li>{@code /tpaall} — invite every online player to teleport to you
 *       (each accepts with {@code /tpaall accept}).</li>
 *   <li>{@code /tempipban <player> <duration>} — temporary IP ban with an expiry.</li>
 * </ul>
 *
 * <p>Each command requires {@code sessentials.<command>} (default op). The module is
 * fully self-contained: the {@code /tpaall} invite registry and the temporary IP-ban
 * registry live in this package, and the async pre-login check only ever reads a
 * {@link java.util.concurrent.ConcurrentHashMap} snapshot (Folia-safe).</p>
 */
public final class CmiExtrasModule implements EssModule {

    @Override
    public String name() {
        return "cmiextras";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        TpaAllInvites invites = new TpaAllInvites();
        TempIpBans bans = new TempIpBans(plugin.stores().get("tempipban"));

        plugin.getServer().getPluginManager().registerEvents(new TempIpBanLoginListener(bans), plugin);
        plugin.getServer().getPluginManager().registerEvents(new TpaAllQuitListener(invites), plugin);

        plugin.commands(reg -> {
            OpListCommand.register(reg);
            ColorsCommand.register(reg);
            TpoCommand.register(plugin, reg);
            TpaAllCommand.register(plugin, reg, invites);
            TempIpBanCommand.register(plugin, reg, bans);
        });
    }
}
