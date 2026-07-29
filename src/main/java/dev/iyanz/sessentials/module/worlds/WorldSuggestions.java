package dev.iyanz.sessentials.module.worlds;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tab-completion providers for the {@code /world} command family: loaded world
 * names, creatable environments, and gamerule names — each filtered by the
 * currently typed prefix.
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class WorldSuggestions {

    /** Environments a new world may be created with. */
    private static final World.Environment[] CREATABLE_ENVIRONMENTS =
            {World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END};

    /** Suggests the name of every currently loaded world. */
    static final SuggestionProvider<CommandSourceStack> LOADED_WORLDS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(world.getName());
            }
        }
        return builder.buildFuture();
    };

    /** Suggests the environments a new world can be created with. */
    static final SuggestionProvider<CommandSourceStack> ENVIRONMENTS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (World.Environment environment : CREATABLE_ENVIRONMENTS) {
            if (environment.name().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(environment.name());
            }
        }
        return builder.buildFuture();
    };

    /** Suggests every known {@link GameRule} name. */
    static final SuggestionProvider<CommandSourceStack> GAME_RULES = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (GameRule<?> rule : GameRule.values()) {
            if (rule.getName().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(rule.getName());
            }
        }
        return builder.buildFuture();
    };

    private WorldSuggestions() {
    }
}
