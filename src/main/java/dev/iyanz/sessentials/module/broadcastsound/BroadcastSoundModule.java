package dev.iyanz.sessentials.module.broadcastsound;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.api.EssModule;

/**
 * Server-wide sound broadcast module: {@code /bsound <sound> [volume] [pitch]} plays
 * a Minecraft sound to every online player, each at their own location.
 *
 * <p>The command is gated behind the {@code sessentials.bsound} operator permission and
 * resolves the requested sound key against the live {@link org.bukkit.Registry#SOUNDS}
 * registry, so any vanilla (or resource-pack registered) sound key is playable. See
 * {@link BroadcastSoundCommands} for the command wiring and {@link SoundSuggestions} for
 * the curated tab-completion list.</p>
 */
public final class BroadcastSoundModule implements EssModule {

    @Override
    public String name() {
        return "broadcastsound";
    }

    @Override
    public void enable(SEssentialsPlugin plugin) {
        BroadcastSoundCommands.register(plugin);
    }
}
