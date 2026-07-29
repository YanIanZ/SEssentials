package dev.iyanz.sessentials.module.deathmessages;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Restyles vanilla player death messages: recolors them with a small skull prefix,
 * or hides them entirely, depending on config.
 *
 * <p>Registers a single {@link DeathMessagesListener} that reads
 * {@code deathmessages.enabled} (default {@code true}) and {@code deathmessages.hide}
 * (default {@code false}) fresh on every death, so an operator reloading the config
 * takes effect immediately. No command is needed for this module.</p>
 */
public final class DeathMessagesModule implements EssModule {

    @Override
    public String name() {
        return "deathmessages";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new DeathMessagesListener(plugin), plugin);
    }
}
