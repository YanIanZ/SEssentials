package dev.iyanz.sessentials.module.itemcmds;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

/**
 * Enchantment registry helpers for this package: key-based lookup, tab suggestions
 * and human-readable display names, all backed by the server's data-driven
 * enchantment registry (so datapack enchantments work too).
 */
@SuppressWarnings("UnstableApiUsage")
final class Enchants {

    /** The live enchantment registry obtained through {@link RegistryAccess}. */
    static final Registry<Enchantment> REGISTRY =
            RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

    /** Tab-completes the plain key of every registered enchantment, filtered by prefix. */
    static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (Enchantment enchantment : REGISTRY) {
            String key = enchantment.getKey().getKey();
            if (key.startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    private Enchants() {
    }

    /**
     * Looks up an enchantment by a plain key ({@code "mending"}) or a namespaced one
     * ({@code "minecraft:mending"}), case-insensitively.
     *
     * @param input the typed enchantment argument
     * @return the registered enchantment, or {@code null} if the key is unknown/invalid
     */
    static Enchantment resolve(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        NamespacedKey key = lower.indexOf(':') >= 0
                ? NamespacedKey.fromString(lower)
                : NamespacedKey.minecraft(lower);
        return key == null ? null : REGISTRY.get(key);
    }

    /**
     * Turns an enchantment's key into a readable name, e.g. {@code sweeping_edge}
     * becomes {@code Sweeping Edge}.
     *
     * @param enchantment the enchantment to name
     * @return a Title Cased, space-separated display name
     */
    static String displayName(Enchantment enchantment) {
        String[] parts = enchantment.getKey().getKey().split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
