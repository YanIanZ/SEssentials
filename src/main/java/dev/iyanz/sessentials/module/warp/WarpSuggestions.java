package dev.iyanz.sessentials.module.warp;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tab-completes existing warp names (for {@code /warp} and {@code /delwarp}),
 * filtered by the currently typed prefix. Warps are global, so suggestions are
 * offered to any sender (players and console alike).
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class WarpSuggestions {

    private WarpSuggestions() {
    }

    /**
     * @param plugin the owning plugin (for warp-store access)
     * @return a suggestion provider listing existing warp names, filtered by the
     *         currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> of(SEssentialsPlugin plugin) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String name : Warps.names(plugin)) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
