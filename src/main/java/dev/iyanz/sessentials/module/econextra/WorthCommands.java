package dev.iyanz.sessentials.module.econextra;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import dev.iyanz.sessentials.SEssentialsPlugin;
import dev.iyanz.sessentials.command.Cmds;
import dev.iyanz.sessentials.economy.EconomyHook;
import dev.iyanz.sessentials.util.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /worth [amount]} (prices the item in the sender's hand, unit price times
 * either the held stack size or a given hypothetical amount) and {@code /sellhand}
 * (alias {@code /sell hand}, which sells the held item for that same worth). Prices
 * come from {@code worth.<MATERIAL_NAME>} in {@code config.yml}; an item with no
 * configured price can be neither priced nor sold.
 */
@SuppressWarnings("UnstableApiUsage")
final class WorthCommands {

    private WorthCommands() {
    }

    static void register(SEssentialsPlugin plugin) {
        plugin.commands(reg -> {
            reg.register(Commands.literal("worth")
                    .requires(s -> s.getSender().hasPermission("sessentials.worth"))
                    .executes(ctx -> worth(plugin, ctx, -1))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> worth(plugin, ctx, IntegerArgumentType.getInteger(ctx, "amount"))))
                    .build(), "Show the sell-worth of the item in your hand");

            reg.register(Commands.literal("sellhand")
                    .requires(s -> s.getSender().hasPermission("sessentials.sellhand"))
                    .executes(Cmds.playerExec(p -> sellHand(plugin, p)))
                    .build(), "Sell the item in your hand for its configured worth");

            reg.register(Commands.literal("sell")
                    .requires(s -> s.getSender().hasPermission("sessentials.sellhand"))
                    .then(Commands.literal("hand")
                            .executes(Cmds.playerExec(p -> sellHand(plugin, p))))
                    .build(), "Alias for /sellhand");
        });
    }

    /** @return {@code false} (after messaging {@code player}) if no economy provider is present. */
    private static boolean requireEconomy(SEssentialsPlugin plugin, Player player) {
        if (!plugin.economy().available()) {
            Msg.err(player, "No economy provider found.");
            return false;
        }
        return true;
    }

    private static int worth(SEssentialsPlugin plugin, CommandContext<CommandSourceStack> ctx, int overrideAmount) {
        Player player = Cmds.player(ctx);
        if (player == null) {
            return 0;
        }
        if (!requireEconomy(plugin, player)) {
            return 0;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isEmpty()) {
            Msg.err(player, "You are not holding an item.");
            return 0;
        }
        Double unit = WorthService.unitPrice(plugin, hand.getType());
        if (unit == null) {
            Msg.err(player, "That item has no configured worth.");
            return 0;
        }
        int amount = overrideAmount > 0 ? overrideAmount : hand.getAmount();
        double total = unit * amount;
        EconomyHook eco = plugin.economy();
        Msg.value(player, WorthService.displayName(hand.getType()) + " x" + amount + " worth:", eco.format(total));
        return 1;
    }

    private static void sellHand(SEssentialsPlugin plugin, Player player) {
        if (!requireEconomy(plugin, player)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isEmpty()) {
            Msg.err(player, "You are not holding an item.");
            return;
        }
        Double unit = WorthService.unitPrice(plugin, hand.getType());
        if (unit == null) {
            Msg.err(player, "That item has no configured worth.");
            return;
        }
        double total = unit * hand.getAmount();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        EconomyHook eco = plugin.economy();
        eco.deposit(player.getUniqueId(), total);
        Msg.value(player, "Sold " + WorthService.displayName(hand.getType()) + " for:", eco.format(total));
    }
}
