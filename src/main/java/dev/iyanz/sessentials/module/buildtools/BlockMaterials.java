package dev.iyanz.sessentials.module.buildtools;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Shared block-material argument handling for the build tools: tab suggestions over
 * every placeable block type and a forgiving parser with a uniform error message.
 */
@SuppressWarnings("UnstableApiUsage")
final class BlockMaterials {

    /** Tab-completes block material names (lowercase), filtered by the typed prefix. */
    static final SuggestionProvider<CommandSourceStack> BLOCKS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isBlock()) {
                continue;
            }
            String name = material.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private BlockMaterials() {
    }

    /**
     * Resolves a typed name to a block {@link Material}, messaging the player on failure.
     *
     * @param player the player to send the error to
     * @param name   the typed material name (any case, optional {@code minecraft:} prefix)
     * @return the block material, or {@code null} if the name is unknown or not a block
     */
    static Material parse(Player player, String name) {
        Material material = Material.matchMaterial(name);
        if (material == null || material.isLegacy() || !material.isBlock()) {
            Msg.err(player, name + " is not a block type.");
            return null;
        }
        return material;
    }

    /**
     * @param material a material
     * @return the material name in lowercase, for chat output
     */
    static String pretty(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }
}
