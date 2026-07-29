package dev.iyanz.sessentials.module.fun;

import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * {@code /dye <color>}: recolors the sender's held item, if it is a dyeable leather
 * armor piece ({@link LeatherArmorMeta}).
 */
@SuppressWarnings("UnstableApiUsage")
final class DyeCommands {

    private DyeCommands() {
    }

    /**
     * Registers {@code /dye} on {@code plugin}'s command lifecycle.
     *
     * @param plugin the owning plugin
     */
    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("dye")
                        .requires(s -> s.getSender().hasPermission("sessentials.dye"))
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests(FunSuggestions.DYE_COLORS)
                                .executes(DyeCommands::dye))
                        .build(),
                "Dye your held leather armor"));
    }

    private static int dye(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }

        DyeColor color;
        try {
            color = DyeColor.valueOf(StringArgumentType.getString(ctx, "color").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Msg.err(player, "Unknown dye color.");
            return 0;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(player, "You are not holding an item.");
            return 0;
        }

        ItemMeta meta = hand.getItemMeta();
        if (!(meta instanceof LeatherArmorMeta leather)) {
            Msg.err(player, "That item cannot be dyed.");
            return 0;
        }

        leather.setColor(color.getColor());
        hand.setItemMeta(leather);
        Msg.ok(player, "Dyed your " + Words.titleCase(hand.getType().name()) + " " + Words.titleCase(color.name()) + ".");
        return 1;
    }
}
