package dev.iyanz.sessentials.module.holograms;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tab-completes existing hologram ids, filtered by the currently typed prefix.
 * Holograms are global, so suggestions are offered to any sender (players and
 * console alike).
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class HologramSuggestions {

    private HologramSuggestions() {
    }

    /**
     * @param plugin the owning plugin (for hologram-store access)
     * @return a suggestion provider listing existing hologram ids, filtered by the
     *         currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> of(SEssentialsPlugin plugin) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String id : Holograms.ids(plugin)) {
                if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(id);
                }
            }
            return builder.buildFuture();
        };
    }
}
