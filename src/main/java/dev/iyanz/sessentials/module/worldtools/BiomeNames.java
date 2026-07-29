package dev.iyanz.sessentials.module.worldtools;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;

/**
 * Resolves a player-typed name to a {@link Biome} via the {@link Registry#BIOME}
 * registry, and tab-suggests known biome keys for {@code /findbiome}.
 *
 * <p>Accepts either a bare key ({@code "sunflower_plains"}) or a fully namespaced key
 * ({@code "minecraft:sunflower_plains"}); every vanilla biome lives in the
 * {@code minecraft} namespace, so the namespace (if any) is simply stripped before
 * lookup.</p>
 *
 * <p>Uses the legacy {@link Registry#BIOME} constant, which Paper has deprecated in
 * favor of {@code RegistryAccess}; it remains fully functional and is what the
 * simplest lookup path requires, so the deprecation is suppressed deliberately.</p>
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
final class BiomeNames {

    /** Tab-completes known biome keys, filtered by the typed prefix. */
    static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        Registry.BIOME.keyStream()
                .map(NamespacedKey::getKey)
                .filter(key -> key.startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    private BiomeNames() {
    }

    /**
     * Resolves a player-typed biome name.
     *
     * @param raw the typed name, bare or namespaced (e.g. {@code "plains"} or
     *            {@code "minecraft:plains"})
     * @return the matching {@link Biome}, or {@code null} if none matches
     */
    static Biome resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        String path = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;

        Biome direct = Registry.BIOME.get(NamespacedKey.minecraft(path));
        if (direct != null) {
            return direct;
        }
        // Fallback: linear match against the registry key, in case of odd formatting.
        return Registry.BIOME.stream()
                .filter(biome -> biome.getKey().getKey().equalsIgnoreCase(path))
                .findFirst()
                .orElse(null);
    }
}
