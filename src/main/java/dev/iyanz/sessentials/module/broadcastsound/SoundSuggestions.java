package dev.iyanz.sessentials.module.broadcastsound;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tab-completes a curated shortlist of common, useful Minecraft sound keys for
 * {@code /bsound <sound>}, filtered by the currently typed prefix.
 *
 * <p>The list is intentionally small and hand-picked (feedback blips, ambience,
 * fanfares, alerts) rather than the full sound registry, which would flood the
 * completion menu with hundreds of rarely-used entries. Any valid registry key may
 * still be typed by hand — resolution in {@link BroadcastSoundCommands} is not limited
 * to this shortlist.</p>
 */
@ApiStatus.Experimental
@SuppressWarnings("UnstableApiUsage")
final class SoundSuggestions {

    /** Curated, alphabetically grouped set of commonly useful sound keys. */
    private static final List<String> COMMON_SOUNDS = List.of(
            "block.anvil.land",
            "block.bell.use",
            "block.beacon.activate",
            "block.enchantment_table.use",
            "block.note_block.bell",
            "block.note_block.pling",
            "block.portal.travel",
            "entity.ender_dragon.growl",
            "entity.experience_orb.pickup",
            "entity.firework_rocket.blast",
            "entity.lightning_bolt.thunder",
            "entity.player.levelup",
            "entity.villager.no",
            "entity.villager.yes",
            "entity.wither.spawn",
            "item.totem.use",
            "ui.button.click",
            "ui.toast.challenge_complete");

    private SoundSuggestions() {
    }

    /**
     * @return a suggestion provider offering the curated sound keys, filtered by the
     *         currently typed (lower-cased) prefix
     */
    static SuggestionProvider<CommandSourceStack> of() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String key : COMMON_SOUNDS) {
                if (key.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(key);
                }
            }
            return builder.buildFuture();
        };
    }
}
