package dev.iyanz.sessentials.module.itemadmin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /mobhead <type>}: gives the sender the vanilla head/skull item for the
 * mobs that have one as a plain material (zombie, skeleton, wither skeleton,
 * creeper, piglin, dragon, player). The command executor already runs on the
 * sender's region thread, so the inventory is mutated directly; overflow is
 * dropped at the player's feet.
 */
@SuppressWarnings("UnstableApiUsage")
final class MobHeadCommand {

    /** Mob name to vanilla head material, in suggestion order. */
    private static final Map<String, Material> HEADS = buildHeads();

    /** Suggests the supported mob head types, filtered by the typed prefix. */
    private static final SuggestionProvider<CommandSourceStack> TYPES = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String type : HEADS.keySet()) {
            if (type.startsWith(remaining)) {
                builder.suggest(type);
            }
        }
        return builder.buildFuture();
    };

    private MobHeadCommand() {
    }

    /**
     * Registers {@code /mobhead}.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("mobhead")
                        .requires(s -> s.getSender().hasPermission("sessentials.mobhead"))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(TYPES)
                                .executes(MobHeadCommand::give))
                        .build(),
                "Give yourself a vanilla mob head"));
    }

    private static int give(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        String type = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        Material head = HEADS.get(type);
        if (head == null) {
            Msg.err(player, "No vanilla head exists for '" + type + "'. Try: "
                    + String.join(", ", HEADS.keySet()) + ".");
            return 0;
        }
        // Already on the sender's region thread inside the command executor.
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(head));
        for (ItemStack left : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
        Msg.ok(player, "There you go, one " + type.replace('_', ' ') + " head.");
        return 1;
    }

    private static Map<String, Material> buildHeads() {
        Map<String, Material> heads = new LinkedHashMap<>();
        heads.put("zombie", Material.ZOMBIE_HEAD);
        heads.put("skeleton", Material.SKELETON_SKULL);
        heads.put("wither_skeleton", Material.WITHER_SKELETON_SKULL);
        heads.put("creeper", Material.CREEPER_HEAD);
        heads.put("piglin", Material.PIGLIN_HEAD);
        heads.put("dragon", Material.DRAGON_HEAD);
        heads.put("player", Material.PLAYER_HEAD);
        return heads;
    }
}
