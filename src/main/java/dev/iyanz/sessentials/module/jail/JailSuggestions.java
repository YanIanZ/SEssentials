package dev.iyanz.sessentials.module.jail;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Tab-completes existing jail names (for {@code /jail}, {@code /deljail}), filtered by
 * the currently typed prefix.
 */
@SuppressWarnings("UnstableApiUsage")
final class JailSuggestions {

    private JailSuggestions() {
    }

    /**
     * @param plugin the owning plugin (for jail-store access)
     * @return a suggestion provider listing existing jail names, filtered by the
     *         currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> of(SEssentialsPlugin plugin) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String name : Jails.names(plugin)) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
