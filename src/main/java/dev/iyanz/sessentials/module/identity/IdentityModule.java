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
    }
}
