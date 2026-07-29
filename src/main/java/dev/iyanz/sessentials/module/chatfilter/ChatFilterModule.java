package dev.iyanz.sessentials.module.chatfilter;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Screens outgoing chat messages for a configurable list of banned words, blocking or
 * censoring the message on a match.
 *
 * <p>Registration and config reads live in {@link ChatFilterListener}; this class only
 * wires the listener in.</p>
 */
public final class ChatFilterModule implements EssModule {

    @Override
    public String name() {
        return "chatfilter";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ChatFilterListener(plugin), plugin);
    }
}
