package dev.iyanz.sessentials.module.items;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

/**
 * Shared Brigadier tab-suggestion providers and registry access for the items
 * module: item material names and enchantment keys, both filtered by the typed
 * prefix.
 */
final class ItemSuggestions {

    /** The (non-deprecated) enchantment registry, shared with {@link EnchantCommands}. */
    static final Registry<Enchantment> ENCHANTMENT_REGISTRY = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

    /** Suggests every {@link Material} that can back an item stack ({@link Material#isItem()}). */
    static final SuggestionProvider<CommandSourceStack> MATERIALS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (Material material : Material.values()) {
            if (!material.isItem()) {
                continue;
            }
            String name = material.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    /** Suggests every registered {@link Enchantment}'s plain key, plus the {@code all} keyword. */
    static final SuggestionProvider<CommandSourceStack> ENCHANTMENTS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        if ("all".startsWith(remaining)) {
            builder.suggest("all");
        }
        for (Enchantment enchantment : ENCHANTMENT_REGISTRY) {
            String key = enchantment.getKey().getKey();
            if (key.startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    private ItemSuggestions() {
    }
}
