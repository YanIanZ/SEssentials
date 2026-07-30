package dev.iyanz.sessentials.module.buildtools;

import java.util.Locale;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.scheduler.Schedulers;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 * {@code /tree [type]}: grows a tree of the chosen {@link TreeType} on top of the
 * block the player is looking at (within {@value #TARGET_RANGE} blocks).
 *
 * <p>Folia-safe: the target block can sit in a different region than the player, so
 * the {@link World#generateTree(Location, TreeType)} call is dispatched to the region
 * thread that owns the plant location via {@link Bukkit#getRegionScheduler()}, and the
 * result message hops back onto the player's own region thread via
 * {@link Schedulers#entity}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
final class TreePlanter {

    /** Maximum distance (blocks) at which a target block is picked. */
    static final int TARGET_RANGE = 8;

    /** Tab-completes {@link TreeType} names (lowercase), filtered by the typed prefix. */
    static final SuggestionProvider<CommandSourceStack> TREE_TYPES = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (TreeType type : TreeType.values()) {
            String name = type.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private TreePlanter() {
    }

    /**
     * Grows a tree on top of the block the player is looking at.
     *
     * @param plugin   the owning plugin, used for the region-thread dispatch
     * @param player   the sending player
     * @param typeName the requested tree type name, or {@code null} for a plain oak
     *                 {@link TreeType#TREE}
     */
    static void plant(SEssentialsPlugin plugin, Player player, String typeName) {
        TreeType type = TreeType.TREE;
        if (typeName != null) {
            try {
                type = TreeType.valueOf(typeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException notAType) {
                Msg.err(player, typeName + " is not a tree type.");
                return;
            }
        }
        Block target = player.getTargetBlockExact(TARGET_RANGE);
        if (target == null) {
            Msg.err(player, "You are not looking at a block within " + TARGET_RANGE + " blocks.");
            return;
        }
        World world = target.getWorld();
        Location plantAt = target.getRelative(BlockFace.UP).getLocation();
        TreeType chosen = type;
        Bukkit.getRegionScheduler().execute(plugin, plantAt, () -> {
            boolean grown = world.generateTree(plantAt, chosen);
            Schedulers.entity(plugin, player, () -> {
                if (grown) {
                    Msg.ok(player, "Grew a " + chosen.name().toLowerCase(Locale.ROOT) + " tree.");
                } else {
                    Msg.err(player, "The tree could not grow there.");
                }
            });
        });
    }
}
