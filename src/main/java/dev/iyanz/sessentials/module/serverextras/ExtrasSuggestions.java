package dev.iyanz.sessentials.module.serverextras;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.jetbrains.annotations.ApiStatus;

/**
 * Brigadier tab-suggestion providers shared by the server-extras commands: item
 * material names for {@code /giveall} and a curated shortlist of common sound keys
 * for {@code /sound}, both filtered by the currently typed prefix.
 *
 * <p>The sound shortlist is intentionally small — the full sound registry holds
 * hundreds of entries and would swamp the completion menu. Any valid registry key
 * can still be typed by hand; command-side resolution is not limited to this list.</p>
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class ExtrasSuggestions {

    /** Suggests every {@link Material} that can back an item stack, lower-cased. */
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

    /** Hand-picked, commonly useful sound keys (feedback blips, alerts, fanfares). */
    private static final List<String> COMMON_SOUND_KEYS = List.of(
            "block.amethyst_block.chime",
            "block.anvil.land",
            "block.bell.use",
            "block.note_block.bell",
            "block.note_block.pling",
            "entity.arrow.hit_player",
            "entity.ender_dragon.growl",
            "entity.experience_orb.pickup",
            "entity.firework_rocket.launch",
            "entity.lightning_bolt.thunder",
            "entity.player.levelup",
            "entity.villager.no",
            "entity.villager.yes",
            "item.totem.use",
            "ui.button.click",
            "ui.toast.challenge_complete",
            "ui.toast.in");

    /** Suggests the curated sound keys, filtered by the typed prefix. */
    static final SuggestionProvider<CommandSourceStack> SOUND_KEYS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String key : COMMON_SOUND_KEYS) {
            if (key.startsWith(remaining)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    };

    private ExtrasSuggestions() {
    }
}
