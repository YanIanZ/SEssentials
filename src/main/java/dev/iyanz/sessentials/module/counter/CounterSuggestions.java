package dev.iyanz.sessentials.module.counter;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tab-completes existing counter names (for {@code /counter <name> ...}), filtered
 * by the currently typed prefix.
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class CounterSuggestions {

    private CounterSuggestions() {
    }

    /**
     * @param plugin the owning plugin (for counter-store access)
     * @return a suggestion provider listing existing counter names, filtered by the
     *         currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> of(SEssentialsPlugin plugin) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String name : Counters.names(plugin)) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
