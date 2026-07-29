package dev.iyanz.sessentials.module.chatformat;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Applies a configurable, MiniMessage-based chat format to every message via
 * {@link io.papermc.paper.event.player.AsyncChatEvent#renderer}.
 *
 * <p>Disabled by default ({@code chat-format.enabled}) so it never fights another chat
 * plugin, or SEssentials' own {@link dev.iyanz.sessentials.module.chat.ChatListener}
 * chat-colour feature, unless an operator opts in. Registration and config reads live in
 * {@link ChatFormatListener}; this class only wires the listener in.</p>
 */
public final class ChatFormatModule implements EssModule {

    @Override
    public String name() {
        return "chatformat";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new ChatFormatListener(plugin), plugin);
    }
}
