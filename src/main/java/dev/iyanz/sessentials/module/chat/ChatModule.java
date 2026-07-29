package dev.iyanz.sessentials.module.chat;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Chat-utility commands: a persisted per-player {@code /chatcolor}, {@code /clearchat},
 * {@code /actionbar}, {@code /bossbar} and the server-wide {@code /alert}.
 *
 * <p>This module owns a single {@link ChatColorService} — the chat-colour persistence
 * shared by both the {@code /chatcolor} command and the {@link ChatListener} that
 * applies it on every message — and delegates each command's registration to its own
 * class, keeping the individual command implementations small and independent.</p>
 */
public final class ChatModule implements EssModule {

    @Override
    public String name() {
        return "chat";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        ChatColorService colorService = new ChatColorService(plugin);

        plugin.getServer().getPluginManager().registerEvents(new ChatListener(colorService), plugin);

        ChatColorCommand.register(plugin, colorService);
        ClearChatCommand.register(plugin);
        ActionBarCommand.register(plugin);
        BossBarCommand.register(plugin);
        AlertCommand.register(plugin);
    }
}
