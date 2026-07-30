package dev.iyanz.sessentials.module.identity;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Player-identity commands: {@code /nick}, {@code /realname}, {@code /afk} and
 * {@code /seen}. Nicknames and last-seen timestamps are persisted in the
 * {@code "identity"} data store and restored automatically on join.
 *
 * <p>This module owns a single {@link IdentityListener} (join/quit bookkeeping) and
 * delegates each command's registration to its own class, keeping the individual
 * command implementations small and independent.</p>
 */
public final class IdentityModule implements EssModule {

    private AutoAfk autoAfk;

    @Override
    public String name() {
        return "identity";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        NickService nickService = new NickService(plugin);
        AfkService afkService = new AfkService();

        plugin.getServer().getPluginManager()
                .registerEvents(new IdentityListener(plugin, nickService, afkService), plugin);

        NickCommand.register(plugin, nickService);
        RealnameCommand.register(plugin);
        AfkCommand.register(plugin, afkService);
        SeenCommand.register(plugin);

        // CMI-style auto-AFK on idle: marks/unmarks AFK via the same AfkService + broadcast
        // as /afk (never kicks — the afkkick module owns kicking). Disabled when
        // `afk.auto-seconds` is 0.
        this.autoAfk = new AutoAfk(plugin, afkService);
        this.autoAfk.start();
    }

    @Override
    public void disable(SEssentialsPlugin plugin) {
        if (autoAfk != null) {
            autoAfk.stop();
            autoAfk = null;
        }
    }
}
