package dev.iyanz.sessentials.module.portals;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Tab-completion providers for the {@code /portal} command tree: existing portal
 * names (for {@code portal delete}) and valid destinations — {@code "spawn"} plus
 * existing warp names (for {@code portal create}).
 */
@SuppressWarnings("UnstableApiUsage")
final class PortalSuggestions {

    private PortalSuggestions() {
    }

    /**
     * @param registry the portal registry to read names from
     * @return a suggestion provider listing existing portal names, filtered by the
     *         currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> names(PortalRegistry registry) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String name : registry.names()) {
                if (name.startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    /**
     * @param plugin the owning plugin (for warp-store access)
     * @return a suggestion provider listing {@code "spawn"} plus existing warp names,
     *         filtered by the currently typed prefix
     */
    static SuggestionProvider<CommandSourceStack> destinations(SEssentialsPlugin plugin) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            if ("spawn".startsWith(remaining)) {
                builder.suggest("spawn");
            }
            for (String name : plugin.stores().get("warp").keys("warps")) {
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
