package dev.iyanz.sessentials.module.spawner;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.EntityType;

/**
 * Tab-suggests and resolves the {@link EntityType} names accepted by {@code /spawner}.
 *
 * <p>Only {@linkplain EntityType#isSpawnable() spawnable} types are offered as
 * suggestions, since a mob spawner can only meaningfully spawn those. Resolution itself
 * accepts any entity-type name case-insensitively and leaves the spawnable check to the
 * caller, so a command can report a precise reason ("unknown type" vs "not spawnable")
 * on failure.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class SpawnerTypes {

    /** Tab-completes spawnable entity-type names, filtered by the typed prefix. */
    static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (EntityType type : EntityType.values()) {
            String name = type.name().toLowerCase(Locale.ROOT);
            if (type.isSpawnable() && name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private SpawnerTypes() {
    }

    /**
     * Resolves a player-typed name to an {@link EntityType}, case-insensitively.
     *
     * @param raw the typed name (e.g. {@code "zombie"} or {@code "ZOMBIE"})
     * @return the matching {@link EntityType}, or {@code null} if no such type exists
     */
    static EntityType resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
