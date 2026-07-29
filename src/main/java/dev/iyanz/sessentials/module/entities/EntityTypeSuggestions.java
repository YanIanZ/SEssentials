package dev.iyanz.sessentials.module.entities;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.EntityType;

/**
 * Tab-completion for {@link EntityType} names on the {@code /killentities} command.
 * Suggests every entity type in lower case, minus {@code PLAYER} (which is never a
 * removal target) and the sentinel {@code UNKNOWN} type.
 */
@SuppressWarnings("UnstableApiUsage")
public final class EntityTypeSuggestions {

    /** Suggests removable {@link EntityType} names, filtered by the typed prefix. */
    public static final SuggestionProvider<CommandSourceStack> SUGGEST = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (EntityType type : EntityType.values()) {
            if (type == EntityType.PLAYER || type.name().equals("UNKNOWN")) {
                continue;
            }
            String name = type.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private EntityTypeSuggestions() {
    }
}
