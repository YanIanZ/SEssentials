package dev.iyanz.sessentials.module.messaging;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Player-to-player and server-wide chat features: private messages ({@code /msg} and
 * {@code /reply}), a moderator "social spy" toggle, server-wide broadcasts and the
 * classic {@code /me} action line.
 *
 * <p>This module owns the two pieces of shared, in-memory state its commands need:
 * who each online player last privately messaged (so {@code /reply} knows who to
 * answer) and which players currently have social spy enabled. Both are process-local
 * and are not persisted to disk.</p>
 */
public final class MessagingModule implements EssModule {

    private final ConcurrentHashMap<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    @Override
    public String name() {
        return "messaging";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        plugin.getServer().getPluginManager()
                .registerEvents(new MessagingQuitListener(lastMessaged, spies), plugin);
        new PrivateMessaging(lastMessaged, spies).register(plugin);
        BroadcastCommands.register(plugin);
    }
}
