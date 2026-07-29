package dev.iyanz.sessentials.module.items;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * {@code /rename} (alias {@code /itemname}) and {@code /lore}: set the held item's
 * display name / lore from operator-supplied MiniMessage text (hex colours, gradients
 * etc. all work). Italic is explicitly disabled so the text renders exactly as
 * written — Minecraft italicises custom item names/lore by default.
 */
@SuppressWarnings("UnstableApiUsage")
final class ItemTextCommands {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ItemTextCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> reg.register(
                Commands.literal("rename")
                        .requires(s -> s.getSender().hasPermission("sessentials.rename"))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ItemTextCommands::rename))
                        .build(),
                "Rename the item in your hand",
                List.of("itemname")));

        plugin.commands(reg -> reg.register(
                Commands.literal("lore")
                        .requires(s -> s.getSender().hasPermission("sessentials.lore"))
                        .then(Commands.literal("clear").executes(ItemTextCommands::clearLore))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ItemTextCommands::lore))
                        .build(),
                "Set (or clear) the item in your hand's lore"));
    }

    private static int rename(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        ItemMeta meta = hand.getItemMeta();
        meta.displayName(deserialize(StringArgumentType.getString(ctx, "text")));
        hand.setItemMeta(meta);
        Msg.ok(player, "Item renamed.");
        return 1;
    }

    private static int lore(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        ItemMeta meta = hand.getItemMeta();
        meta.lore(List.of(deserialize(StringArgumentType.getString(ctx, "text"))));
        hand.setItemMeta(meta);
        Msg.ok(player, "Lore set.");
        return 1;
    }

    private static int clearLore(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        ItemStack hand = heldItemOrError(player);
        if (hand == null) {
            return 0;
        }
        ItemMeta meta = hand.getItemMeta();
        meta.lore(null);
        hand.setItemMeta(meta);
        Msg.ok(player, "Lore cleared.");
        return 1;
    }

    private static ItemStack heldItemOrError(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Msg.err(player, "You are not holding an item.");
            return null;
        }
        return hand;
    }

    private static Component deserialize(String miniMessage) {
        return MM.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }
}
