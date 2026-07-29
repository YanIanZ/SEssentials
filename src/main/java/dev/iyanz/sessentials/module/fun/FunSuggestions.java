package dev.iyanz.sessentials.module.fun;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

/**
 * Shared Brigadier tab-suggestion providers and registry lookups for the fun module:
 * potion effect type keys (plus the {@link #CLEAR_KEYWORD}) and dye color names.
 */
final class FunSuggestions {

    /** Word sent to {@code /effect} to remove every active potion effect instead of applying one. */
    static final String CLEAR_KEYWORD = "clear";

    /** Suggests every registered {@link PotionEffectType} key, plus {@link #CLEAR_KEYWORD}. */
    static final SuggestionProvider<CommandSourceStack> EFFECT_TYPES = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        if (CLEAR_KEYWORD.startsWith(remaining)) {
            builder.suggest(CLEAR_KEYWORD);
        }
        for (PotionEffectType type : Registry.EFFECT) {
            String key = type.getKey().getKey();
            if (key.startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    /** Suggests every {@link DyeColor} name, lower-cased. */
    static final SuggestionProvider<CommandSourceStack> DYE_COLORS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (DyeColor color : DyeColor.values()) {
            String name = color.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private FunSuggestions() {
    }

    /**
     * Resolves a potion effect type from a plain (unnamespaced) key such as
     * {@code "speed"}, looked up in the {@code minecraft} namespace via
     * {@link Registry#EFFECT}.
     *
     * @param key the plain key, as typed by a player
     * @return the matching type, or {@code null} if none is registered under that key
     */
    static PotionEffectType resolveEffectType(String key) {
        return Registry.EFFECT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
    }
}
