package dev.iyanz.sessentials.module.butcher;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;

/**
 * Selects which category of living entity {@code /butcher} sweeps up.
 *
 * <p>The category only narrows an already safety-filtered set (players, tamed pets,
 * named mobs and armor stands are never touched — see {@link ButcherModule}); it does
 * not by itself expose protected entities. The one exception is villagers, which are
 * spared for every category except {@link #ALL}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
enum ButcherType {

    /** Every non-player living entity that survives the safety filter (villagers included). */
    ALL("all", "entity", "entities"),

    /** Hostile / neutral {@link Monster}s only. This is the default when no type is given. */
    MONSTERS("monsters", "monster", "monsters"),

    /** Passive {@link Animals} only (cows, sheep, chickens, untamed wolves, and the like). */
    ANIMALS("animals", "animal", "animals");

    /** Tab-completes the three keywords, filtered by the currently typed prefix. */
    static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (ButcherType type : values()) {
            if (type.keyword.startsWith(remaining)) {
                builder.suggest(type.keyword);
            }
        }
        return builder.buildFuture();
    };

    private final String keyword;
    private final String singular;
    private final String plural;

    ButcherType(String keyword, String singular, String plural) {
        this.keyword = keyword;
        this.singular = singular;
        this.plural = plural;
    }

    /**
     * Parses a user-supplied keyword into a category.
     *
     * @param raw the raw argument (any case)
     * @return the matching category, or {@code null} if the keyword is unknown
     */
    static ButcherType parse(String raw) {
        String key = raw.toLowerCase(Locale.ROOT);
        for (ButcherType type : values()) {
            if (type.keyword.equals(key)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Tests whether {@code entity} belongs to this category. Callers must apply the
     * shared safety filter first; this method only checks the entity's kind.
     *
     * @param entity the (already non-player) living entity to classify
     * @return {@code true} if the entity is in this category
     */
    boolean matches(LivingEntity entity) {
        return switch (this) {
            case ALL -> true;
            case MONSTERS -> entity instanceof Monster;
            case ANIMALS -> entity instanceof Animals;
        };
    }

    /**
     * @param count how many entities were removed
     * @return the correctly pluralized noun for this category
     */
    String noun(int count) {
        return count == 1 ? singular : plural;
    }
}
